package com.jenos.gestionecole.model;

import java.time.LocalDate;

/**
 * FeeInstallment — Échéance individuelle d'un frais scolaire affecté à un élève.
 *
 * Chaque StudentFeeAssignment peut comporter une ou plusieurs échéances selon
 * la fréquence de paiement configurée (ANNUEL → 1, TRIMESTRIEL → 3, MENSUEL → 10).
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class FeeInstallment {

    public enum InstallmentStatus {
        NON_PAYEE("Non payée"),
        PARTIELLEMENT_PAYEE("Partiellement payée"),
        PAYEE("Payée");

        private final String label;

        InstallmentStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private int id;
    private int assignmentId;
    private int installmentNumber; // 1, 2, 3...
    private String label;          // ex: "1er Trimestre", "Octobre 2025"
    private LocalDate dueDate;
    private double amount;
    private double amountPaid;
    private String currency;       // "USD" ou "CDF"
    private InstallmentStatus status;

    public FeeInstallment() {
        this.status = InstallmentStatus.NON_PAYEE;
        this.amountPaid = 0.0;
    }

    public FeeInstallment(int id, int assignmentId, int installmentNumber, String label,
                          LocalDate dueDate, double amount, String currency) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.installmentNumber = installmentNumber;
        this.label = label;
        this.dueDate = dueDate;
        this.amount = amount;
        this.amountPaid = 0.0;
        this.currency = currency;
        this.status = InstallmentStatus.NON_PAYEE;
    }

    // ── Méthodes métier ──────────────────────────────────────────

    /** Solde restant sur cette échéance. */
    public double getBalance() {
        return Math.max(0, amount - amountPaid);
    }

    /** L'échéance est en retard (date dépassée et non entièrement payée). */
    public boolean isOverdue() {
        return status != InstallmentStatus.PAYEE
                && dueDate != null
                && LocalDate.now().isAfter(dueDate);
    }

    /** Enregistre un paiement sur cette échéance et met à jour le statut. */
    public void addPayment(double paymentAmount) {
        if (paymentAmount <= 0) return;
        this.amountPaid += paymentAmount;
        updateStatus();
    }

    private void updateStatus() {
        if (amountPaid >= amount) {
            this.amountPaid = amount;
            this.status = InstallmentStatus.PAYEE;
        } else if (amountPaid > 0) {
            this.status = InstallmentStatus.PARTIELLEMENT_PAYEE;
        } else {
            this.status = InstallmentStatus.NON_PAYEE;
        }
    }

    // ── Getters & Setters ────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(int installmentNumber) { this.installmentNumber = installmentNumber; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
        updateStatus();
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public InstallmentStatus getStatus() { return status; }
    public void setStatus(InstallmentStatus status) { this.status = status; }

    @Override
    public String toString() {
        return label + " : " + String.format("%.2f", amount) + " " + currency
                + " (" + status.getLabel() + ")";
    }
}
