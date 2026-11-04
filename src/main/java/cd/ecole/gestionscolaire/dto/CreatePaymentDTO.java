package cd.ecole.gestionscolaire.dto;

/**
 * CreatePaymentDTO — Objet de transfert pour la demande d'enregistrement d'un paiement.
 */
public class CreatePaymentDTO {

    private long studentId;
    private Long staffId;
    private Long feeId;
    private double amount;
    private String currency; // 'USD' ou 'CDF'
    private String paymentMethod; // 'ESPECES', 'MOBILE_MONEY', 'VIREMENT', 'CHEQUE', 'AUTRE'
    private String reference;
    private String justification;
    private String cashierUsername;
    private String cashRegisterCode;

    public CreatePaymentDTO() {}

    public CreatePaymentDTO(long studentId, Long feeId, double amount, String currency, String paymentMethod, String reference, String cashierUsername, String cashRegisterCode) {
        this.studentId = studentId;
        this.feeId = feeId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.reference = reference;
        this.cashierUsername = cashierUsername;
        this.cashRegisterCode = cashRegisterCode;
    }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }

    public Long getFeeId() { return feeId; }
    public void setFeeId(Long feeId) { this.feeId = feeId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public String getCashRegisterCode() { return cashRegisterCode; }
    public void setCashRegisterCode(String cashRegisterCode) { this.cashRegisterCode = cashRegisterCode; }
}
