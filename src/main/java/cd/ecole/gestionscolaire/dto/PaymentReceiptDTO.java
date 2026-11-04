package cd.ecole.gestionscolaire.dto;

import java.time.LocalDateTime;

/**
 * PaymentReceiptDTO — Quittance officielle de paiement générée suite à un encaissement valide.
 */
public class PaymentReceiptDTO {

    private long paymentId;
    private String receiptNumber;
    private LocalDateTime issuedAt;
    private String schoolName;
    private String studentFullName;
    private String studentMatricule;
    private String academicStructure; // Ex: "1ère Scientifique A - Secondaire"
    private String feeLabel;
    private double amountPaid;
    private String currency; // 'USD' ou 'CDF'
    private String paymentMethod;
    private String transactionReference;
    private double totalFeeDue;
    private double balanceRemaining;
    private String cashierName;
    private String cashRegisterName;

    public PaymentReceiptDTO() {}

    public long getPaymentId() { return paymentId; }
    public void setPaymentId(long paymentId) { this.paymentId = paymentId; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public String getStudentFullName() { return studentFullName; }
    public void setStudentFullName(String studentFullName) { this.studentFullName = studentFullName; }

    public String getStudentMatricule() { return studentMatricule; }
    public void setStudentMatricule(String studentMatricule) { this.studentMatricule = studentMatricule; }

    public String getAcademicStructure() { return academicStructure; }
    public void setAcademicStructure(String academicStructure) { this.academicStructure = academicStructure; }

    public String getFeeLabel() { return feeLabel; }
    public void setFeeLabel(String feeLabel) { this.feeLabel = feeLabel; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public double getTotalFeeDue() { return totalFeeDue; }
    public void setTotalFeeDue(double totalFeeDue) { this.totalFeeDue = totalFeeDue; }

    public double getBalanceRemaining() { return balanceRemaining; }
    public void setBalanceRemaining(double balanceRemaining) { this.balanceRemaining = balanceRemaining; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public String getCashRegisterName() { return cashRegisterName; }
    public void setCashRegisterName(String cashRegisterName) { this.cashRegisterName = cashRegisterName; }
}
