package com.jenos.gestionecole.model;

import java.time.LocalDateTime;

/**
 * Operation — Enregistrement immuable d'un mouvement financier ou opération de caisse.
 * RÈGLE : Historique non destructif. Aucune suppression physique n'est permise.
 */
public class Operation {

    public enum OperationType {
        PAIEMENT_ELEVE,
        PAIEMENT_PERSONNEL,
        DEPENSE,
        CLOTURE_CAISSE,
        AJUSTEMENT
    }

    public enum OperationStatus {
        VALIDEE,
        ANNULEE,
        CORRIGEE
    }

    private int id;
    private String referenceNumber;
    private OperationType type;
    private String description;
    private double amount;
    private String currency; // "USD" ou "CDF"
    private String paymentMethod;
    private String transactionReference;
    private String cashierUsername;
    private String cashRegisterName;
    private LocalDateTime dateTime;
    private OperationStatus status;

    // Hiérarchie scolaire & Bénéficiaire
    private String beneficiaryName;
    private String studentMatricule;
    private String sectionName;
    private String promotionName;
    private String optionName;
    private String className;

    // Audit d'annulation ou correction
    private String cancelReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private Integer correctedOperationId;
    private String correctionNotes;

    public Operation() {
        this.status = OperationStatus.VALIDEE;
        this.dateTime = LocalDateTime.now();
    }

    public Operation(int id, String referenceNumber, OperationType type, String description,
                     double amount, String currency, String paymentMethod, String transactionReference,
                     String cashierUsername, String cashRegisterName, LocalDateTime dateTime, OperationStatus status) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.type = type;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.transactionReference = transactionReference;
        this.cashierUsername = cashierUsername;
        this.cashRegisterName = cashRegisterName;
        this.dateTime = dateTime;
        this.status = status;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public OperationType getType() { return type; }
    public void setType(OperationType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public String getCashRegisterName() { return cashRegisterName; }
    public void setCashRegisterName(String cashRegisterName) { this.cashRegisterName = cashRegisterName; }

    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

    public OperationStatus getStatus() { return status; }
    public void setStatus(OperationStatus status) { this.status = status; }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public String getStudentMatricule() { return studentMatricule; }
    public void setStudentMatricule(String studentMatricule) { this.studentMatricule = studentMatricule; }

    public String getSectionName() { return sectionName != null ? sectionName : "—"; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getPromotionName() { return promotionName != null ? promotionName : "—"; }
    public void setPromotionName(String promotionName) { this.promotionName = promotionName; }

    public String getOptionName() { return optionName != null ? optionName : "—"; }
    public void setOptionName(String optionName) { this.optionName = optionName; }

    public String getClassName() { return className != null ? className : "—"; }
    public void setClassName(String className) { this.className = className; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public Integer getCorrectedOperationId() { return correctedOperationId; }
    public void setCorrectedOperationId(Integer correctedOperationId) { this.correctedOperationId = correctedOperationId; }

    public String getCorrectionNotes() { return correctionNotes; }
    public void setCorrectionNotes(String correctionNotes) { this.correctionNotes = correctionNotes; }
}
