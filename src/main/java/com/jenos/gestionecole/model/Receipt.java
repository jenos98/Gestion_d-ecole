package com.jenos.gestionecole.model;

import java.time.LocalDateTime;

/**
 * Receipt — Quittance officielle de paiement de caisse (Reçu élève ou personnel).
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class Receipt {

    private int id;
    private String receiptNumber; // ex: REC-2025-0042
    private Payment payment;
    private LocalDateTime issuedAt;
    private String cashierUsername;
    private String cashRegisterName;

    // Bénéficiaire
    private String beneficiaryName;
    private String beneficiaryMatricule;
    private String beneficiaryDetail; // ex: "6e Math-Physique A - Scientifique" ou "Enseignant Mathématiques"

    // Détail financier
    private String feeOrPurpose;       // ex: "Minerval 1er Trimestre"
    private double amountPaid;
    private String currency;           // "USD" ou "CDF"
    private String paymentMethod;      // "ESPECES", "MOBILE_MONEY", "VIREMENT", etc.
    private String transactionReference;

    // Solde après paiement (dans la même devise !)
    private double feeTotalDue;
    private double feePreviouslyPaid;
    private double balanceRemaining;

    // Statut d'impression (original vs duplicata)
    private boolean duplicate;
    private int printCount;

    public Receipt() {
        this.issuedAt = LocalDateTime.now();
        this.duplicate = false;
        this.printCount = 1;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public String getCashRegisterName() { return cashRegisterName; }
    public void setCashRegisterName(String cashRegisterName) { this.cashRegisterName = cashRegisterName; }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public String getBeneficiaryMatricule() { return beneficiaryMatricule; }
    public void setBeneficiaryMatricule(String beneficiaryMatricule) { this.beneficiaryMatricule = beneficiaryMatricule; }

    public String getBeneficiaryDetail() { return beneficiaryDetail; }
    public void setBeneficiaryDetail(String beneficiaryDetail) { this.beneficiaryDetail = beneficiaryDetail; }

    public String getFeeOrPurpose() { return feeOrPurpose; }
    public void setFeeOrPurpose(String feeOrPurpose) { this.feeOrPurpose = feeOrPurpose; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public double getFeeTotalDue() { return feeTotalDue; }
    public void setFeeTotalDue(double feeTotalDue) { this.feeTotalDue = feeTotalDue; }

    public double getFeePreviouslyPaid() { return feePreviouslyPaid; }
    public void setFeePreviouslyPaid(double feePreviouslyPaid) { this.feePreviouslyPaid = feePreviouslyPaid; }

    public double getBalanceRemaining() { return balanceRemaining; }
    public void setBalanceRemaining(double balanceRemaining) { this.balanceRemaining = balanceRemaining; }

    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }

    public int getPrintCount() { return printCount; }
    public void setPrintCount(int printCount) { this.printCount = printCount; }
}
