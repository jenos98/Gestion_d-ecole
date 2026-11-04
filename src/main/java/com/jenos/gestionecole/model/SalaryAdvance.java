package com.jenos.gestionecole.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SalaryAdvance — Enregistrement et suivi individuel d'une avance sur salaire.
 *
 * RÈGLES MÉTIER :
 * - Suivi individuel du montant initial et du solde restant.
 * - Monnaie strictement cloisonnée (USD ou CDF).
 * - Déduction progressive avec protection anti-double déduction et limitation au solde restant.
 */
public class SalaryAdvance {

    public enum AdvanceStatus {
        EN_COURS("En cours"),
        REMBOURSEE("Remboursée");

        private final String label;

        AdvanceStatus(String label) {
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

    public static class AdvanceDeduction {
        private final LocalDate date;
        private final double amount;
        private final String salaryRecordNumber;
        private final String note;

        public AdvanceDeduction(LocalDate date, double amount, String salaryRecordNumber, String note) {
            this.date = date;
            this.amount = amount;
            this.salaryRecordNumber = salaryRecordNumber;
            this.note = note;
        }

        public LocalDate getDate() { return date; }
        public double getAmount() { return amount; }
        public String getSalaryRecordNumber() { return salaryRecordNumber; }
        public String getNote() { return note; }
    }

    private int id;
    private int staffId;
    private String staffMatricule;
    private String staffFullName;
    private double initialAmount;
    private double remainingBalance;
    private String currency; // "USD" ou "CDF"
    private LocalDate grantedDate;
    private String reason;
    private AdvanceStatus status = AdvanceStatus.EN_COURS;
    private String approvedBy;
    private final List<AdvanceDeduction> deductions = new ArrayList<>();

    public SalaryAdvance() {}

    public SalaryAdvance(int id, int staffId, String staffMatricule, String staffFullName,
                         double initialAmount, String currency, LocalDate grantedDate,
                         String reason, String approvedBy) {
        this.id = id;
        this.staffId = staffId;
        this.staffMatricule = staffMatricule;
        this.staffFullName = staffFullName;
        this.initialAmount = Math.round(initialAmount);
        this.remainingBalance = this.initialAmount;
        this.currency = (currency != null) ? currency.toUpperCase().trim() : "USD";
        this.grantedDate = (grantedDate != null) ? grantedDate : LocalDate.now();
        this.reason = reason;
        this.approvedBy = approvedBy;
        this.status = (this.remainingBalance <= 0) ? AdvanceStatus.REMBOURSEE : AdvanceStatus.EN_COURS;
    }

    /**
     * Déduit un montant de l'avance en vérifiant le solde disponible.
     * @return le montant effectivement déduit
     */
    public synchronized double applyDeduction(double requestedAmount, String salaryRecordRef, String note) {
        if (requestedAmount <= 0 || status == AdvanceStatus.REMBOURSEE) {
            return 0.0;
        }
        double toDeduct = Math.min(requestedAmount, remainingBalance);
        toDeduct = Math.round(toDeduct); // Arrondi à l'unité monétaire
        remainingBalance -= toDeduct;
        deductions.add(new AdvanceDeduction(LocalDate.now(), toDeduct, salaryRecordRef, note));

        if (remainingBalance <= 0.001) {
            remainingBalance = 0.0;
            status = AdvanceStatus.REMBOURSEE;
        }
        return toDeduct;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }

    public String getStaffMatricule() { return staffMatricule; }
    public void setStaffMatricule(String staffMatricule) { this.staffMatricule = staffMatricule; }

    public String getStaffFullName() { return staffFullName; }
    public void setStaffFullName(String staffFullName) { this.staffFullName = staffFullName; }

    public double getInitialAmount() { return initialAmount; }
    public void setInitialAmount(double initialAmount) {
        this.initialAmount = Math.round(initialAmount);
    }

    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) {
        this.remainingBalance = Math.round(remainingBalance);
        if (this.remainingBalance <= 0.001) {
            this.remainingBalance = 0.0;
            this.status = AdvanceStatus.REMBOURSEE;
        }
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) {
        this.currency = (currency != null) ? currency.toUpperCase().trim() : "USD";
    }

    public LocalDate getGrantedDate() { return grantedDate; }
    public void setGrantedDate(LocalDate grantedDate) { this.grantedDate = grantedDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public AdvanceStatus getStatus() { return status; }
    public void setStatus(AdvanceStatus status) { this.status = status; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public List<AdvanceDeduction> getDeductions() { return new ArrayList<>(deductions); }
}
