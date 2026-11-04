package com.jenos.gestionecole.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SalaryPayment — Enregistrement d'un versement (intégral ou partiel) sur une rémunération validée.
 *
 * RÈGLES MÉTIER :
 * - Associe le versement à la rémunération validée, à l'employé et à la période.
 * - Monnaie strictement cloisonnée (USD ou CDF).
 * - Traçabilité de l'utilisateur ayant validé l'opération.
 */
public class SalaryPayment {

    private int id;
    private int salaryRecordId;
    private int staffId;
    private String staffMatricule;
    private String staffFullName;
    private String period;
    private double amount;
    private String currency; // "USD" ou "CDF"
    private LocalDate paymentDate;
    private LocalDateTime recordedAt;
    private String paymentMethod; // "ESPECES", "VIREMENT", "MOBILE_MONEY", "CHEQUE"
    private String reference;
    private String recordedByUsername;
    private String remarks;

    public SalaryPayment() {
        this.paymentDate = LocalDate.now();
        this.recordedAt = LocalDateTime.now();
    }

    public SalaryPayment(int id, int salaryRecordId, int staffId, String staffMatricule,
                         String staffFullName, String period, double amount, String currency,
                         LocalDate paymentDate, String paymentMethod, String reference,
                         String recordedByUsername, String remarks) {
        this.id = id;
        this.salaryRecordId = salaryRecordId;
        this.staffId = staffId;
        this.staffMatricule = staffMatricule;
        this.staffFullName = staffFullName;
        this.period = period;
        this.amount = Math.round(amount); // Arrondi à l'unité monétaire
        this.currency = (currency != null) ? currency.toUpperCase().trim() : "USD";
        this.paymentDate = (paymentDate != null) ? paymentDate : LocalDate.now();
        this.recordedAt = LocalDateTime.now();
        this.paymentMethod = paymentMethod;
        this.reference = reference;
        this.recordedByUsername = recordedByUsername;
        this.remarks = remarks;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSalaryRecordId() { return salaryRecordId; }
    public void setSalaryRecordId(int salaryRecordId) { this.salaryRecordId = salaryRecordId; }

    public int getStaffId() { return staffId; }
    public void setStaffId(int staffId) { this.staffId = staffId; }

    public String getStaffMatricule() { return staffMatricule; }
    public void setStaffMatricule(String staffMatricule) { this.staffMatricule = staffMatricule; }

    public String getStaffFullName() { return staffFullName; }
    public void setStaffFullName(String staffFullName) { this.staffFullName = staffFullName; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = Math.round(amount); }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) {
        this.currency = (currency != null) ? currency.toUpperCase().trim() : "USD";
    }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getRecordedByUsername() { return recordedByUsername; }
    public void setRecordedByUsername(String recordedByUsername) { this.recordedByUsername = recordedByUsername; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
