package com.jenos.gestionecole;

import com.jenos.gestionecole.model.CashSession;
import com.jenos.gestionecole.model.Expense;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.service.CashRegisterStateManager;
import com.jenos.gestionecole.service.ExpenseService;
import com.jenos.gestionecole.util.RoleGuard;

import java.time.LocalDate;
import java.util.List;

/**
 * CashRegisterModuleTest — Suite de validation automatisée du Bloc 4 :
 * 1. Séparation stricte USD / CDF
 * 2. Contrôle de solde en caisse lors des dépenses
 * 3. Seuil d'autorisation préalable des dépenses importantes
 * 4. Workflow obligatoire du rapport journalier avant clôture
 * 5. Détection des écarts de comptage physique et motifs obligatoires
 * 6. Permissions RoleGuard de caisse
 * 7. Non-destructivité des dépenses
 */
public class CashRegisterModuleTest {

    public static void main(String[] args) {
        System.out.println("=== DÉBUT DES TESTS BLOC 4 (Cycle de Caisse, Dépenses & Clôture) ===");

        testStrictCurrencySeparation();
        testExpenseCashBalanceCheck();
        testExpenseApprovalThreshold();
        testDailyReportMandatoryWorkflow();
        testCountingVarianceAndReason();
        testExpenseCancellationNonDestructive();
        testRoleGuardCashPermissions();

        System.out.println("=== TOUS LES TESTS DU BLOC 4 ONT RÉUSSI (100%) ===");
    }

    private static void testStrictCurrencySeparation() {
        System.out.print("[TEST 1] Séparation stricte USD / CDF (Soldes & Flux)... ");
        CashRegisterStateManager state = CashRegisterStateManager.getInstance();
        CashSession session = CashClosureService.getInstance().getCurrentSession();
        assert session != null : "Session de caisse active requise";

        double balUSD = session.getCurrentBalanceUSD();
        double balCDF = session.getCurrentBalanceCDF();
        double inUSD = state.getTotalInUSD();
        double inCDF = state.getTotalInCDF();

        assert balUSD >= 0 : "Solde USD positif ou nul";
        assert balCDF >= 0 : "Solde CDF positif ou nul";
        assert inUSD >= 0 : "Entrées USD distinctes";
        assert inCDF >= 0 : "Entrées CDF distinctes";

        // Simulation d'une entrée USD : n'affecte QUE l'USD
        double initialUSD = state.getTotalInUSD();
        double initialCDF = state.getTotalInCDF();
        state.addIncome(50.0, "USD");
        assert state.getTotalInUSD() == initialUSD + 50.0 : "Entrée USD incrémente USD";
        assert state.getTotalInCDF() == initialCDF : "Entrée USD n'altère JAMAIS le CDF";

        System.out.println("OK");
    }

    private static void testExpenseCashBalanceCheck() {
        System.out.print("[TEST 2] Contrôle backend du solde disponible en caisse... ");
        ExpenseService expService = ExpenseService.getInstance();
        CashSession session = CashClosureService.getInstance().getCurrentSession();

        Expense excessExpense = new Expense();
        excessExpense.setCategory("Maintenance");
        excessExpense.setLabel("Gros travaux");
        excessExpense.setBeneficiary("Entreprise BTP");
        excessExpense.setAmount(session.getCurrentBalanceUSD() + 5000.0); // Dépassera forcément
        excessExpense.setCurrency("USD");
        excessExpense.setPaymentMethod(Expense.PaymentMethod.ESPECES);
        excessExpense.setExpenseDate(LocalDate.now());

        boolean rejected = false;
        try {
            expService.processExpense(excessExpense);
        } catch (IllegalArgumentException e) {
            rejected = true;
            assert e.getMessage().contains("SOLDE INSUFFISANT") : "Message d'erreur solde insuffisant";
        }
        assert rejected : "Une dépense excédant le solde en caisse doit être impérativement rejetée par le backend";

        System.out.println("OK");
    }

    private static void testExpenseApprovalThreshold() {
        System.out.print("[TEST 3] Seuil d'autorisation pour les dépenses importantes... ");
        ExpenseService expService = ExpenseService.getInstance();

        Expense largeExpense = new Expense();
        largeExpense.setCategory("Matériel pédagogique");
        largeExpense.setLabel("Achat projecteurs");
        largeExpense.setBeneficiary("TechStore");
        largeExpense.setAmount(ExpenseService.APPROVAL_THRESHOLD_USD + 50.0);
        largeExpense.setCurrency("USD");
        largeExpense.setPaymentMethod(Expense.PaymentMethod.ESPECES);
        largeExpense.setExpenseDate(LocalDate.now());

        Expense processed = expService.processExpense(largeExpense);
        assert processed.getStatus() == Expense.ExpenseStatus.EN_ATTENTE :
                "Une dépense supérieure au seuil doit passer en statut EN_ATTENTE d'autorisation";

        System.out.println("OK");
    }

    private static void testDailyReportMandatoryWorkflow() {
        System.out.print("[TEST 4] Workflow obligatoire du rapport journalier avant clôture... ");
        CashRegisterStateManager state = CashRegisterStateManager.getInstance();

        // 1. Initialement : non généré -> pas prêt
        state.openNewSession(100.0, 200000.0, "Jenos");
        assert !state.isReadyForNormalClosure() : "La caisse ne doit PAS être prête sans rapport";
        assert state.getReportStatus() == CashRegisterStateManager.ReportStatus.NON_GENERE : "Statut initial NON_GENERE";

        // 2. Rapport généré mais non imprimé -> toujours pas prêt pour la clôture normale !
        CashRegisterStateManager.DailyReportData report = state.generateDailyReport();
        assert report != null : "Le rapport doit être généré";
        assert report.getReportNumber().startsWith("RAP-") : "Numéro de rapport conforme";
        assert state.getReportStatus() == CashRegisterStateManager.ReportStatus.GENERE : "Statut GENERE";
        assert !state.isReadyForNormalClosure() : "Généré seul ne suffit PAS pour autoriser la clôture";

        // 3. Rapport imprimé -> débloqué pour la clôture normale !
        state.markReportPrinted();
        assert state.getReportStatus() == CashRegisterStateManager.ReportStatus.IMPRIME : "Statut IMPRIME";
        assert state.isReadyForNormalClosure() : "Le rapport imprimé DOIT débloquer la clôture normale";

        System.out.println("OK");
    }

    private static void testCountingVarianceAndReason() {
        System.out.print("[TEST 5] Rapprochement, calcul d'écarts et motifs... ");
        CashRegisterStateManager state = CashRegisterStateManager.getInstance();
        CashSession session = CashClosureService.getInstance().getCurrentSession();

        double theoCDF = session.getCurrentBalanceCDF();
        double theoUSD = session.getCurrentBalanceUSD();

        // Simulation d'un comptage avec écart en CDF (-5000 CDF) et conforme en USD
        double countedCDF = theoCDF - 5000.0;
        double countedUSD = theoUSD;

        state.recordCounting(countedCDF, countedUSD, "Erreur de rendu monnaie sur encaissement", "");
        assert state.getCurrentState() == CashRegisterStateManager.CashRegisterState.ECART_DETECTE :
                "Un écart de comptage doit faire basculer l'état en ECART_DETECTE";
        assert "Erreur de rendu monnaie sur encaissement".equals(state.getVarianceReasonCDF()) :
                "Le motif d'écart CDF doit être fidèlement enregistré";

        // Confirmation de clôture
        state.confirmClosure("Clôture régulière de fin de journée");
        assert state.getCurrentState() == CashRegisterStateManager.CashRegisterState.CLOTUREE :
                "Après confirmation, la caisse doit être CLOTUREE";
        assert !state.isCashOpen() : "La caisse ne doit plus être ouverte";

        System.out.println("OK");
    }

    private static void testExpenseCancellationNonDestructive() {
        System.out.print("[TEST 6] Annulation non destructive d'une dépense... ");
        ExpenseService expService = ExpenseService.getInstance();
        List<Expense> expenses = expService.findAll();
        assert !expenses.isEmpty() : "Au moins une dépense doit exister";

        Expense exp = expenses.get(0);
        int expId = exp.getId();

        boolean cancelled = expService.cancelExpense(expId);
        assert cancelled : "L'annulation doit réussir";

        Expense updated = expService.findById(expId);
        assert updated != null : "L'enregistrement ne doit PAS être supprimé de la base";
        assert updated.getStatus() == Expense.ExpenseStatus.ANNULEE : "Le statut doit être ANNULEE";

        System.out.println("OK");
    }

    private static void testRoleGuardCashPermissions() {
        System.out.print("[TEST 7] Permissions RoleGuard de caisse et clôture... ");

        // Rôle Caissière
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assert RoleGuard.canOpenCashRegister() : "La caissière peut ouvrir sa caisse";
        assert RoleGuard.canViewOwnCashRegister() : "La caissière peut voir sa caisse";
        assert !RoleGuard.canViewAllCashRegisters() : "La caissière ne peut PAS voir toutes les caisses";
        assert RoleGuard.canGenerateDailyReport() : "La caissière peut générer le rapport journalier";
        assert RoleGuard.canPrintDailyReport() : "La caissière peut imprimer le rapport journalier";
        assert RoleGuard.canCloseCashRegister() : "La caissière peut procéder à la clôture normale";
        assert !RoleGuard.canForceCloseCashRegister() : "La caissière ne peut PAS forcer une clôture exceptionnelle";

        // Rôle Promoteur
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);
        assert RoleGuard.canViewAllCashRegisters() : "Le promoteur peut voir toutes les caisses";
        assert RoleGuard.canForceCloseCashRegister() : "Le promoteur peut ordonner une clôture exceptionnelle";
        assert RoleGuard.canAuthorizeVariance() : "Le promoteur peut autoriser les écarts";

        RoleGuard.clearRole();
        System.out.println("OK");
    }
}
