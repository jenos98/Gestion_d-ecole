package cd.ecole.gestionscolaire.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PaymentDTO — Données de consultation d'un paiement.
 */
public class PaymentDTO {

    private long id;
    private String receiptNumber;
    private long studentId;
    private String studentName;
    private String studentMatricule;
    private String className;
    private String feeName;
    private double amount;
    private String currency;
    private String paymentMethod;
    private String reference;
    private String status; // 'VALIDEE', 'ANNULEE', 'CORRIGEE'
    private String cashierUsername;
    private String cashRegisterName;
    private LocalDate paymentDate;
    private LocalDateTime createdAt;

    public PaymentDTO() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentMatricule() { return studentMatricule; }
    public void setStudentMatricule(String studentMatricule) { this.studentMatricule = studentMatricule; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getFeeName() { return feeName; }
    public void setFeeName(String feeName) { this.feeName = feeName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public String getCashRegisterName() { return cashRegisterName; }
    public void setCashRegisterName(String cashRegisterName) { this.cashRegisterName = cashRegisterName; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
