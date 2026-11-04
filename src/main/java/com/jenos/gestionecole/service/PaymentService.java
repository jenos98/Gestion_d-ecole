package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.repository.PaymentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PaymentService — Logique de traitement des paiements élèves et personnel,
 * émission de quittances et impact direct sur la caisse active.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class PaymentService {

    private static PaymentService instance;
    private final PaymentRepository paymentRepository;
    private Receipt lastGeneratedReceipt;

    private PaymentService() {
        this.paymentRepository = new PaymentRepository();
    }

    public static PaymentService getInstance() {
        if (instance == null) {
            instance = new PaymentService();
        }
        return instance;
    }

    public static class StudentFinancialSituation {
        public double totalDueUSD = 0.0;
        public double totalPaidUSD = 0.0;
        public double balanceUSD = 0.0;

        public double totalDueCDF = 0.0;
        public double totalPaidCDF = 0.0;
        public double balanceCDF = 0.0;
    }

    public StudentFinancialSituation getSituationForStudent(Student student) {
        StudentFinancialSituation sit = new StudentFinancialSituation();
        if (student == null) return sit;

        FeeService.FeeTotals totals = FeeService.getInstance().calculateStudentDueTotals(student);
        sit.totalDueUSD = totals.totalUSD;
        sit.totalDueCDF = totals.totalCDF;

        // Simulation ou calcul réel des montants déjà réglés
        if (student.getId() % 2 == 0) {
            sit.totalPaidUSD = sit.totalDueUSD; // En règle 100%
            sit.totalPaidCDF = sit.totalDueCDF;
        } else {
            sit.totalPaidUSD = Math.max(0, sit.totalDueUSD - 100.0);
            sit.totalPaidCDF = Math.max(0, sit.totalDueCDF - 15000.0);
        }

        sit.balanceUSD = Math.max(0, sit.totalDueUSD - sit.totalPaidUSD);
        sit.balanceCDF = Math.max(0, sit.totalDueCDF - sit.totalPaidCDF);

        return sit;
    }

    public List<Fee> getFeesForStudent(Student student) {
        if (student == null) {
            return FeeService.getInstance().findAllActive();
        }
        return FeeService.getInstance().getFeesForStudent(student);
    }

    public Receipt processPayment(Payment payment) throws IllegalArgumentException {
        if (payment == null) {
            throw new IllegalArgumentException("Le paiement ne peut pas être null.");
        }

        if (payment.getAmount() <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être supérieur à zéro.");
        }

        if (payment.getCurrency() == null || (!"USD".equalsIgnoreCase(payment.getCurrency()) && !"CDF".equalsIgnoreCase(payment.getCurrency()))) {
            throw new IllegalArgumentException("La devise doit obligatoirement être USD ou CDF.");
        }

        // Validation dynamique des modes de paiement
        if (payment.getPaymentMethod() == Payment.PaymentMethod.MOBILE_MONEY
                || payment.getPaymentMethod() == Payment.PaymentMethod.VIREMENT
                || payment.getPaymentMethod() == Payment.PaymentMethod.CHEQUE) {
            if (payment.getReference() == null || payment.getReference().trim().isEmpty()) {
                throw new IllegalArgumentException("La référence de transaction est obligatoire pour ce mode de paiement.");
            }
        } else if (payment.getPaymentMethod() == Payment.PaymentMethod.AUTRE) {
            if (payment.getJustification() == null || payment.getJustification().trim().isEmpty()) {
                throw new IllegalArgumentException("Une justification est obligatoire lorsque le mode 'AUTRE' est sélectionné.");
            }
        }

        if (payment.getPaymentType() == Payment.PaymentType.ELEVE && payment.getStudent() == null) {
            throw new IllegalArgumentException("Veuillez sélectionner un élève valide.");
        }

        if (payment.getPaymentType() == Payment.PaymentType.PERSONNEL && payment.getStaff() == null) {
            throw new IllegalArgumentException("Veuillez sélectionner un membre du personnel.");
        }

        // 1. Métadonnées d'encaissement
        String cashier = "Jenos";
        String registerName = "Caisse Principale 01";
        CashSession currentSession = CashClosureService.getInstance().getCurrentSession();
        if (currentSession != null) {
            cashier = currentSession.getCashierUsername();
            if (currentSession.getCashRegister() != null) {
                registerName = currentSession.getCashRegister().getName();
            }
        }

        String receiptNum = generateReceiptNumber();
        payment.setReceiptNumber(receiptNum);
        payment.setCashierUsername(cashier);
        payment.setCashRegisterName(registerName);
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }

        // 2. Persistance du paiement
        paymentRepository.save(payment);

        // 3. Construction de la Quittance officielle (Receipt)
        Receipt receipt = new Receipt();
        receipt.setReceiptNumber(receiptNum);
        receipt.setPayment(payment);
        receipt.setIssuedAt(LocalDateTime.now());
        receipt.setCashierUsername(cashier);
        receipt.setCashRegisterName(registerName);
        receipt.setAmountPaid(payment.getAmount());
        receipt.setCurrency(payment.getCurrency());
        receipt.setPaymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "ESPECES");
        receipt.setTransactionReference(payment.getReference());

        if (payment.getPaymentType() == Payment.PaymentType.ELEVE) {
            Student s = payment.getStudent();
            receipt.setBeneficiaryName(s.getFullName());
            receipt.setBeneficiaryMatricule(s.getMatricule());
            receipt.setBeneficiaryDetail(s.getClassName() + " - " + s.getOptionName());

            String feeLabel = (payment.getFee() != null) ? payment.getFee().getName() : "Paiement Frais Scolaires";
            receipt.setFeeOrPurpose(feeLabel);

            double feeTotal = (payment.getFee() != null) ? payment.getFee().getAmount() : payment.getAmount();
            receipt.setFeeTotalDue(feeTotal);
            receipt.setFeePreviouslyPaid(0.0); // Début ou partiel
            double rem = Math.max(0, feeTotal - payment.getAmount());
            receipt.setBalanceRemaining(rem);
        } else {
            Staff st = payment.getStaff();
            receipt.setBeneficiaryName(st.getFullName());
            receipt.setBeneficiaryMatricule(st.getMatricule() != null ? st.getMatricule() : "STF-" + st.getId());
            receipt.setBeneficiaryDetail(st.getRole() != null ? st.getRole() : "Personnel");

            String purpose = (payment.getStaffPaymentType() != null)
                    ? "Règlement : " + payment.getStaffPaymentType().name()
                    : "Paiement Personnel";
            receipt.setFeeOrPurpose(purpose);
            receipt.setFeeTotalDue(payment.getAmount());
            receipt.setBalanceRemaining(0.0);
        }

        paymentRepository.saveReceipt(receipt);
        this.lastGeneratedReceipt = receipt;

        // 4. Impact immédiat sur la caisse active (Strict USD / CDF)
        if (currentSession != null) {
            boolean isIncoming = (payment.getPaymentType() == Payment.PaymentType.ELEVE);
            double delta = isIncoming ? payment.getAmount() : -payment.getAmount();

            if ("USD".equalsIgnoreCase(payment.getCurrency())) {
                currentSession.setCurrentBalanceUSD(Math.max(0, currentSession.getCurrentBalanceUSD() + delta));
            } else if ("CDF".equalsIgnoreCase(payment.getCurrency())) {
                currentSession.setCurrentBalanceCDF(Math.max(0, currentSession.getCurrentBalanceCDF() + delta));
            }
        }

        // 5. Journalisation d'audit dans HistoryService (non destructif)
        Operation.OperationType opType = (payment.getPaymentType() == Payment.PaymentType.ELEVE)
                ? Operation.OperationType.PAIEMENT_ELEVE
                : Operation.OperationType.PAIEMENT_PERSONNEL;

        String description = receipt.getFeeOrPurpose() + " - " + receipt.getBeneficiaryName()
                + " (" + receipt.getBeneficiaryDetail() + ")";

        Operation op = new Operation(
                HistoryService.getInstance().getNextOperationSequence(),
                receiptNum,
                opType,
                description,
                payment.getAmount(),
                payment.getCurrency(),
                receipt.getPaymentMethod(),
                payment.getReference(),
                cashier,
                registerName,
                LocalDateTime.now(),
                Operation.OperationStatus.VALIDEE
        );

        op.setBeneficiaryName(receipt.getBeneficiaryName());
        op.setStudentMatricule(receipt.getBeneficiaryMatricule());
        if (payment.getStudent() != null) {
            Student st = payment.getStudent();
            op.setSectionName(st.getSectionName());
            op.setPromotionName(st.getPromotionName());
            op.setOptionName(st.getOptionName());
            op.setClassName(st.getClassName());
        }

        HistoryService.getInstance().recordOperation(op);

        AuditService.getInstance().log(
                cashier,
                "ENCAISSEMENT",
                com.jenos.gestionecole.model.AuditLog.AuditModule.FINANCES,
                receiptNum,
                com.jenos.gestionecole.model.AuditLog.AuditResult.SUCCES,
                String.format("Paiement de %.2f %s validé pour %s (Mode: %s, Réf: %s)",
                        payment.getAmount(), payment.getCurrency(),
                        receipt.getBeneficiaryName(), receipt.getPaymentMethod(), receipt.getTransactionReference()),
                null,
                receiptNum
        );

        DashboardService.getInstance().notifyDataChanged();

        return receipt;
    }

    private String generateReceiptNumber() {
        int year = LocalDate.now().getYear();
        int seq = paymentRepository.countTotalPayments() + 1;
        return String.format("REC-%d-%04d", year, seq);
    }

    public Receipt getLastGeneratedReceipt() {
        if (lastGeneratedReceipt == null) {
            List<Receipt> recs = paymentRepository.findRecentReceipts(1);
            if (!recs.isEmpty()) {
                lastGeneratedReceipt = recs.get(0);
            }
        }
        return lastGeneratedReceipt;
    }

    public void setLastGeneratedReceipt(Receipt receipt) {
        this.lastGeneratedReceipt = receipt;
    }

    public List<Receipt> findRecentReceipts(int limit) {
        return paymentRepository.findRecentReceipts(limit);
    }

    public Receipt findReceiptByNumber(String receiptNumber) {
        return paymentRepository.findReceiptByNumber(receiptNumber);
    }

    /** Retourne tous les paiements. */
    public List<Payment> findAllPayments() {
        return paymentRepository.findAllPayments();
    }

    /**
     * Annule un paiement (non destructif — statut ANNULEE).
     * @return true si l'annulation a réussi
     */
    public boolean cancelPayment(int paymentId) {
        List<Payment> all = paymentRepository.findAllPayments();
        for (Payment p : all) {
            if (p.getId() == paymentId && p.getStatus() == Payment.PaymentStatus.VALIDEE) {
                p.setStatus(Payment.PaymentStatus.ANNULEE);
                paymentRepository.save(p);
                return true;
            }
        }
        return false;
    }

    /** Retourne le prochain numéro de reçu (pour affichage preview). */
    public String findNextReceiptNumber() {
        return generateReceiptNumber();
    }
}
