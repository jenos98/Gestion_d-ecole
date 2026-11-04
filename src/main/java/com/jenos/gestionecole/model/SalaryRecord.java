package com.jenos.gestionecole.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SalaryRecord — Enregistrement complet et immuable d'une rémunération.
 *
 * RÈGLES MÉTIER :
 * - Formule officielle :
 *   Salaire net = Salaire de base + Primes fixes + Primes variables - Retenues - Avances déduites
 * - Solde restant = Salaire net validé - Total des paiements enregistrés
 * - Arrondi à l'unité monétaire.
 * - Immuabilité : une fois validée, la rémunération conserve ses composantes sans altération,
 *   même si le salaire de base de l'employé change ultérieurement.
 * - Statut de paiement : NON_PAYE, PARTIELLEMENT_PAYE, PAYE_INTEGRALEMENT.
 */
public class SalaryRecord {

    public enum ValidationStatus {
        PREPAREE("Préparée"),
        VALIDEE("Validée"),
        CLOTUREE("Clôturée");

        private final String label;
        ValidationStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum PaymentStatus {
        NON_PAYE("Non payé"),
        PARTIELLEMENT_PAYE("Partiellement payé"),
        PAYE_INTEGRALEMENT("Payé intégralement");

        private final String label;
        PaymentStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum Frequency {
        MENSUELLE("Mensuelle"),
        QUINZAINIERE("Quinzainière"),
        HEBDOMADAIRE("Hebdomadaire"),
        PONCTUELLE("Ponctuelle");

        private final String label;
        Frequency(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    private int id;
    private String recordNumber; // Ex: SAL-2026-09-001
    private int staffId;
    private String staffMatricule;
    private String staffFullName;
    private String staffFunction;
    private String staffDepartment;
    private Frequency frequency = Frequency.MENSUELLE;
    private String period; // Ex: "Septembre 2026"
    private int month;
    private int year;
    private String currency; // "USD" ou "CDF"

    // Composantes de calcul (figées lors de la validation)
    private double baseSalary = 0.0;
    private double fixedAllowances = 0.0;
    private double variableAllowances = 0.0;
    private double deductions = 0.0;
    private double advancesDeducted = 0.0;
    private double netSalary = 0.0;

    // Statut de validation & sécurité
    private ValidationStatus validationStatus = ValidationStatus.PREPAREE;
    private String validatedBy;
    private LocalDateTime validatedAt;

    // Suivi financier & versements
    private double totalPaid = 0.0;
    private double remainingBalance = 0.0;
    private PaymentStatus paymentStatus = PaymentStatus.NON_PAYE;
    private final List<SalaryPayment> payments = new ArrayList<>();

    private String remarks;
    private LocalDateTime createdAt = LocalDateTime.now();

    public SalaryRecord() {}

    public SalaryRecord(int id, String recordNumber, Staff staff, String period, int month, int year,
                        Frequency frequency, double baseSalary, double fixedAllowances,
                        double variableAllowances, double deductions, double advancesDeducted) {
        this.id = id;
        this.recordNumber = recordNumber;
        if (staff != null) {
            this.staffId = staff.getId();
            this.staffMatricule = staff.getMatricule();
            this.staffFullName = staff.getFullName();
            this.staffFunction = staff.getFonction();
            this.staffDepartment = staff.getCategorie();
            this.currency = staff.getDeviseSalaire();
        } else {
            this.currency = "USD";
        }
        this.period = period;
        this.month = month;
        this.year = year;
        this.frequency = (frequency != null) ? frequency : Frequency.MENSUELLE;

        // Arrondi à l'unité monétaire de chaque composante
        this.baseSalary = Math.round(baseSalary);
        this.fixedAllowances = Math.round(fixedAllowances);
        this.variableAllowances = Math.round(variableAllowances);
        this.deductions = Math.round(deductions);
        this.advancesDeducted = Math.round(advancesDeducted);

        recalculateNet();
        this.remainingBalance = this.netSalary;
        this.totalPaid = 0.0;
        this.paymentStatus = PaymentStatus.NON_PAYE;
        this.validationStatus = ValidationStatus.PREPAREE;
    }

    /**
     * Calcule le salaire net selon la formule officielle :
     * Net = Base + Primes fixes + Primes variables - Retenues - Avances
     */
    public final void recalculateNet() {
        double totalPositive = baseSalary + fixedAllowances + variableAllowances;
        double totalNegative = deductions + advancesDeducted;
        this.netSalary = Math.max(0.0, Math.round(totalPositive - totalNegative));
        this.remainingBalance = Math.max(0.0, Math.round(this.netSalary - this.totalPaid));
        updatePaymentStatus();
    }

    /**
     * Enregistre un versement (complet ou partiel) et met à jour le solde restant.
     */
    public synchronized void recordPayment(SalaryPayment payment) {
        if (payment == null) return;
        if (validationStatus != ValidationStatus.VALIDEE && validationStatus != ValidationStatus.CLOTUREE) {
            throw new IllegalStateException("Impossible d'enregistrer un paiement sur une rémunération non validée.");
        }
        double amount = Math.round(payment.getAmount());
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être supérieur à zéro.");
        }
        if (amount > remainingBalance) {
            throw new IllegalArgumentException(String.format(
                "Le montant versé (%.0f %s) excède le solde restant dû (%.0f %s).",
                amount, currency, remainingBalance, currency
            ));
        }

        payments.add(payment);
        totalPaid = Math.round(totalPaid + amount);
        remainingBalance = Math.max(0.0, Math.round(netSalary - totalPaid));
        updatePaymentStatus();
    }

    private void updatePaymentStatus() {
        if (totalPaid <= 0.001) {
            this.paymentStatus = PaymentStatus.NON_PAYE;
        } else if (remainingBalance <= 0.001) {
            this.paymentStatus = PaymentStatus.PAYE_INTEGRALEMENT;
            this.validationStatus = ValidationStatus.CLOTUREE;
        } else {
            this.paymentStatus = PaymentStatus.PARTIELLEMENT_PAYE;
        }
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRecordNumber() { return recordNumber; }
    public void setRecordNumber(String recordNumber) { this.recordNumber = recordNumber; }

    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }

    public String getStaffMatricule() { return staffMatricule; }
    public void setStaffMatricule(String staffMatricule) { this.staffMatricule = staffMatricule; }

    public String getStaffFullName() { return staffFullName; }
    public void setStaffFullName(String staffFullName) { this.staffFullName = staffFullName; }

    public String getStaffFunction() { return staffFunction; }
    public void setStaffFunction(String staffFunction) { this.staffFunction = staffFunction; }

    public String getStaffDepartment() { return staffDepartment; }
    public void setStaffDepartment(String staffDepartment) { this.staffDepartment = staffDepartment; }

    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) {
        this.currency = (currency != null) ? currency.toUpperCase().trim() : "USD";
    }

    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) {
        if (validationStatus == ValidationStatus.VALIDEE || validationStatus == ValidationStatus.CLOTUREE) {
            throw new IllegalStateException("Rémunération validée immuable : impossible de modifier le salaire de base.");
        }
        this.baseSalary = Math.round(baseSalary);
        recalculateNet();
    }

    public double getFixedAllowances() { return fixedAllowances; }
    public void setFixedAllowances(double fixedAllowances) {
        if (validationStatus == ValidationStatus.VALIDEE || validationStatus == ValidationStatus.CLOTUREE) {
            throw new IllegalStateException("Rémunération validée immuable : impossible de modifier les primes fixes.");
        }
        this.fixedAllowances = Math.round(fixedAllowances);
        recalculateNet();
    }

    public double getVariableAllowances() { return variableAllowances; }
    public void setVariableAllowances(double variableAllowances) {
        if (validationStatus == ValidationStatus.VALIDEE || validationStatus == ValidationStatus.CLOTUREE) {
            throw new IllegalStateException("Rémunération validée immuable : impossible de modifier les primes variables.");
        }
        this.variableAllowances = Math.round(variableAllowances);
        recalculateNet();
    }

    public double getDeductions() { return deductions; }
    public void setDeductions(double deductions) {
        if (validationStatus == ValidationStatus.VALIDEE || validationStatus == ValidationStatus.CLOTUREE) {
            throw new IllegalStateException("Rémunération validée immuable : impossible de modifier les retenues.");
        }
        this.deductions = Math.round(deductions);
        recalculateNet();
    }

    public double getAdvancesDeducted() { return advancesDeducted; }
    public void setAdvancesDeducted(double advancesDeducted) {
        if (validationStatus == ValidationStatus.VALIDEE || validationStatus == ValidationStatus.CLOTUREE) {
            throw new IllegalStateException("Rémunération validée immuable : impossible de modifier les avances déduites.");
        }
        this.advancesDeducted = Math.round(advancesDeducted);
        recalculateNet();
    }

    public double getNetSalary() { return netSalary; }

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) { this.validatedBy = validatedBy; }

    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }

    public double getTotalPaid() { return totalPaid; }
    public double getRemainingBalance() { return remainingBalance; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public List<SalaryPayment> getPayments() { return new ArrayList<>(payments); }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
