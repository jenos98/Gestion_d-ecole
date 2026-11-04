package com.jenos.gestionecole.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Expense — Dépense engagée (Décaissement de caisse).
 *
 * RÈGLES ABSOLUES :
 * - Séparation stricte USD / CDF (jamais d'addition)
 * - Aucune suppression physique (statut non destructif)
 * - Traçabilité complète du bénéficiaire, mode et justificatif
 */
public class Expense {

    public enum ExpenseStatus {
        VALIDEE("Validée"),
        EN_ATTENTE("En attente d'autorisation"),
        ANNULEE("Annulée"),
        CORRIGEE("Corrigée");

        private final String label;
        ExpenseStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum PaymentMethod {
        ESPECES,
        VIREMENT,
        MOBILE_MONEY,
        CHEQUE,
        AUTRE
    }

    private int id;
    private String voucherNumber;     // N° Bon de dépense, ex: DEP-2026-0042
    private Budget budget;
    private String category;          // Fournitures, Transport, Maintenance, Électricité, Eau, Communication, Petites dépenses, Autre...
    private String label;             // Libellé court
    private String reason;            // Motif détaillé
    private double amount;
    private String currency;          // "USD" ou "CDF"
    private PaymentMethod paymentMethod = PaymentMethod.ESPECES;
    private String beneficiary;
    private String reference;         // N° facture, reçu
    private String attachmentName;    // Nom de fichier justificatif (facture_001.pdf)
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private String cashierUsername;
    private ExpenseStatus status = ExpenseStatus.VALIDEE;

    public Expense() {
        this.status = ExpenseStatus.VALIDEE;
        this.expenseDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getVoucherNumber() { return voucherNumber; }
    public void setVoucherNumber(String voucherNumber) { this.voucherNumber = voucherNumber; }

    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBeneficiary() { return beneficiary; }
    public void setBeneficiary(String beneficiary) { this.beneficiary = beneficiary; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public ExpenseStatus getStatus() { return status; }
    public void setStatus(ExpenseStatus status) { this.status = status; }
}
