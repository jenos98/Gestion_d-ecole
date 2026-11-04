package com.jenos.gestionecole.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payslip — Bulletin de paie officiel d'un membre du personnel.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class Payslip {

    private int id;
    private String payslipNumber;
    private Staff staff;
    private int month;
    private int year;
    private double baseSalary;
    private double allowances;         // Primes & indemnités
    private double advancesDeducted;   // Avances sur salaire déduites
    private double otherDeductions;    // Retenues diverses
    private double netPayable;         // Net perçu
    private String currency;           // "USD" ou "CDF"
    private LocalDate paymentDate;
    private LocalDateTime generatedAt;
    private String cashierUsername;
    private String remarks;

    public Payslip() {
        this.generatedAt = LocalDateTime.now();
        this.paymentDate = LocalDate.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPayslipNumber() { return payslipNumber; }
    public void setPayslipNumber(String payslipNumber) { this.payslipNumber = payslipNumber; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }

    public double getAllowances() { return allowances; }
    public void setAllowances(double allowances) { this.allowances = allowances; }

    public double getAdvancesDeducted() { return advancesDeducted; }
    public void setAdvancesDeducted(double advancesDeducted) { this.advancesDeducted = advancesDeducted; }

    public double getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(double otherDeductions) { this.otherDeductions = otherDeductions; }

    public double getNetPayable() { return netPayable; }
    public void setNetPayable(double netPayable) { this.netPayable = netPayable; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getPeriodLabel() {
        String[] months = {"", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                           "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        String mStr = (month >= 1 && month <= 12) ? months[month] : String.valueOf(month);
        return mStr + " " + year;
    }
}
