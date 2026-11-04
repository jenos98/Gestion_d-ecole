package com.jenos.gestionecole.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payment — Opération financière de paiement (Élève ou Personnel).
 */
public class Payment {

    public enum PaymentType {
        ELEVE,
        PERSONNEL
    }

    public enum StaffPaymentType {
        SALAIRE,
        PRIME,
        AVANCE,
        INDEMNITE
    }

    public enum PaymentMethod {
        ESPECES,
        VIREMENT,
        MOBILE_MONEY,
        CHEQUE,
        AUTRE
    }

    public enum PaymentStatus {
        VALIDEE,
        ANNULEE,
        CORRIGEE
    }

    private int id;
    private String receiptNumber;
    private PaymentType paymentType;
    private StaffPaymentType staffPaymentType; // Si PERSONNEL
    private Student student;                   // Si ELEVE
    private Staff staff;                       // Si PERSONNEL
    private Fee fee;                           // Si ELEVE
    private double amount;
    private String currency;                   // "USD" ou "CDF"
    private PaymentMethod paymentMethod;
    private String reference;                  // Obligatoire si VIREMENT/MOBILE_MONEY/CHEQUE
    private String justification;              // Obligatoire si AUTRE
    private LocalDate paymentDate;
    private LocalDateTime createdAt;
    private String cashierUsername;
    private String cashRegisterName;
    private PaymentStatus status;

    public Payment() {
        this.status = PaymentStatus.VALIDEE;
        this.createdAt = LocalDateTime.now();
        this.paymentDate = LocalDate.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }

    public StaffPaymentType getStaffPaymentType() { return staffPaymentType; }
    public void setStaffPaymentType(StaffPaymentType staffPaymentType) { this.staffPaymentType = staffPaymentType; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }

    public Fee getFee() { return fee; }
    public void setFee(Fee fee) { this.fee = fee; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public String getCashRegisterName() { return cashRegisterName; }
    public void setCashRegisterName(String cashRegisterName) { this.cashRegisterName = cashRegisterName; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
}
