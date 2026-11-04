package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.CashSession;
import com.jenos.gestionecole.model.Expense;
import com.jenos.gestionecole.model.Operation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ExpenseService — Gestion et engagement des dépenses de fonctionnement.
 *
 * RÈGLES MÉTIER :
 * 1. Séparation stricte USD / CDF
 * 2. Vérification systématique du solde disponible en caisse
 * 3. Dépenses soumises à autorisation au-delà d'un seuil
 * 4. Traçabilité dans le journal des opérations (HistoryService)
 * 5. Aucune suppression physique
 */
public class ExpenseService {

    private static ExpenseService instance;

    // Catégories configurables dynamiquement
    private final List<String> categories = new ArrayList<>(Arrays.asList(
            "Fournitures",
            "Transport",
            "Maintenance",
            "Électricité",
            "Eau",
            "Communication",
            "Petites dépenses",
            "Matériel pédagogique",
            "Autre"
    ));

    private final List<Expense> storage = new ArrayList<>();

    // Seuils nécessitant une autorisation préalable du superviseur
    public static final double APPROVAL_THRESHOLD_USD = 250.0;
    public static final double APPROVAL_THRESHOLD_CDF = 700000.0;

    private ExpenseService() {
        initDefaultExpenses();
    }

    public static ExpenseService getInstance() {
        if (instance == null) {
            instance = new ExpenseService();
        }
        return instance;
    }

    private void initDefaultExpenses() {
        Expense e1 = new Expense();
        e1.setId(1);
        e1.setVoucherNumber("DEP-2026-0001");
        e1.setCategory("Fournitures");
        e1.setLabel("Achat de rames de papier A4");
        e1.setReason("Impression des fiches d'évaluation du 1er trimestre");
        e1.setAmount(65.0);
        e1.setCurrency("USD");
        e1.setBeneficiary("Papeterie Centrale");
        e1.setReference("FAC-8891");
        e1.setPaymentMethod(Expense.PaymentMethod.ESPECES);
        e1.setExpenseDate(LocalDate.now());
        e1.setCashierUsername("Jenos");
        e1.setStatus(Expense.ExpenseStatus.VALIDEE);
        storage.add(e1);
    }

    public List<String> getCategories() {
        return new ArrayList<>(categories);
    }

    public void addCategory(String category) {
        if (category != null && !category.isBlank() && !categories.contains(category)) {
            categories.add(category.trim());
        }
    }

    public List<Expense> findAll() {
        return new ArrayList<>(storage);
    }

    public List<Expense> getRecentExpenses() {
        return storage.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Expense findById(int id) {
        return storage.stream().filter(e -> e.getId() == id).findFirst().orElse(null);
    }

    /**
     * Traite et enregistre une dépense.
     * Effectue le contrôle du solde de caisse disponible dans la devise concernée.
     */
    public Expense processExpense(Expense expense) throws IllegalArgumentException {
        if (expense == null) {
            throw new IllegalArgumentException("La dépense ne peut pas être null.");
        }
        if (expense.getAmount() <= 0) {
            throw new IllegalArgumentException("Le montant de la dépense doit être strictement supérieur à zéro.");
        }
        if (expense.getCategory() == null || expense.getCategory().isBlank()) {
            throw new IllegalArgumentException("La catégorie est obligatoire.");
        }
        if (expense.getLabel() == null || expense.getLabel().isBlank()) {
            throw new IllegalArgumentException("La description / libellé est obligatoire.");
        }
        if (expense.getBeneficiary() == null || expense.getBeneficiary().isBlank()) {
            throw new IllegalArgumentException("Le bénéficiaire est obligatoire.");
        }
        if (expense.getCurrency() == null || (!"USD".equalsIgnoreCase(expense.getCurrency()) && !"CDF".equalsIgnoreCase(expense.getCurrency()))) {
            throw new IllegalArgumentException("La devise doit obligatoirement être USD ou CDF.");
        }

        // 1. Vérification du solde de caisse disponible (Contrôle backend non contournable)
        CashSession currentSession = CashClosureService.getInstance().getCurrentSession();
        if (currentSession != null) {
            double currentBalance = "USD".equalsIgnoreCase(expense.getCurrency())
                    ? currentSession.getCurrentBalanceUSD()
                    : currentSession.getCurrentBalanceCDF();

            if (expense.getAmount() > currentBalance) {
                throw new IllegalArgumentException(String.format(
                        "ALERTE SOLDE INSUFFISANT : Le montant demandé (%.2f %s) excède le solde disponible en caisse (%.2f %s).",
                        expense.getAmount(), expense.getCurrency(), currentBalance, expense.getCurrency()
                ));
            }
        }

        // 2. Détermination du statut : En attente d'autorisation si dépasse le seuil
        boolean requiresApproval = ("USD".equalsIgnoreCase(expense.getCurrency()) && expense.getAmount() > APPROVAL_THRESHOLD_USD)
                || ("CDF".equalsIgnoreCase(expense.getCurrency()) && expense.getAmount() > APPROVAL_THRESHOLD_CDF);

        if (requiresApproval) {
            expense.setStatus(Expense.ExpenseStatus.EN_ATTENTE);
        } else {
            expense.setStatus(Expense.ExpenseStatus.VALIDEE);
        }

        // 3. Attribution des identifiants & métadonnées
        int nextId = storage.stream().mapToInt(Expense::getId).max().orElse(0) + 1;
        expense.setId(nextId);
        expense.setVoucherNumber(String.format("DEP-%d-%04d", LocalDate.now().getYear(), nextId));
        expense.setCreatedAt(LocalDateTime.now());
        if (expense.getCashierUsername() == null || expense.getCashierUsername().isBlank()) {
            expense.setCashierUsername(currentSession != null ? currentSession.getCashierUsername() : "Jenos");
        }

        storage.add(expense);

        // 4. Si validée immédiatement, impacte la caisse active et enregistre l'opération
        if (expense.getStatus() == Expense.ExpenseStatus.VALIDEE) {
            applyExpenseToCash(expense);
        }

        return expense;
    }

    private void applyExpenseToCash(Expense expense) {
        CashSession currentSession = CashClosureService.getInstance().getCurrentSession();
        String registerName = "Caisse Principale 01";
        if (currentSession != null) {
            if ("USD".equalsIgnoreCase(expense.getCurrency())) {
                currentSession.setCurrentBalanceUSD(Math.max(0, currentSession.getCurrentBalanceUSD() - expense.getAmount()));
            } else if ("CDF".equalsIgnoreCase(expense.getCurrency())) {
                currentSession.setCurrentBalanceCDF(Math.max(0, currentSession.getCurrentBalanceCDF() - expense.getAmount()));
            }
            if (currentSession.getCashRegister() != null) {
                registerName = currentSession.getCashRegister().getName();
            }
        }

        // Enregistrer la sortie dans le manager de caisse
        CashRegisterStateManager.getInstance().addExpense(expense.getAmount(), expense.getCurrency());

        // Enregistrer dans le journal d'audit HistoryService
        Operation op = new Operation(
                HistoryService.getInstance().getNextOperationSequence(),
                expense.getVoucherNumber(),
                Operation.OperationType.DEPENSE,
                expense.getCategory() + " : " + expense.getLabel() + " (" + expense.getBeneficiary() + ")",
                expense.getAmount(),
                expense.getCurrency(),
                expense.getPaymentMethod() != null ? expense.getPaymentMethod().name() : "ESPECES",
                expense.getReference(),
                expense.getCashierUsername(),
                registerName,
                LocalDateTime.now(),
                Operation.OperationStatus.VALIDEE
        );
        HistoryService.getInstance().recordOperation(op);
        DashboardService.getInstance().notifyDataChanged();
    }

    /**
     * Annule une dépense (non destructif).
     */
    public boolean cancelExpense(int expenseId) {
        Expense e = findById(expenseId);
        if (e != null && e.getStatus() != Expense.ExpenseStatus.ANNULEE) {
            e.setStatus(Expense.ExpenseStatus.ANNULEE);

            // Re-créditer la caisse si elle avait été validée
            CashSession session = CashClosureService.getInstance().getCurrentSession();
            if (session != null) {
                if ("USD".equalsIgnoreCase(e.getCurrency())) {
                    session.setCurrentBalanceUSD(session.getCurrentBalanceUSD() + e.getAmount());
                } else if ("CDF".equalsIgnoreCase(e.getCurrency())) {
                    session.setCurrentBalanceCDF(session.getCurrentBalanceCDF() + e.getAmount());
                }
            }
            return true;
        }
        return false;
    }
}
