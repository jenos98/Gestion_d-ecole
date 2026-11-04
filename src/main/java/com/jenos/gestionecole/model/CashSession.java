package com.jenos.gestionecole.model;

import java.time.LocalDateTime;

/**
 * CashSession — Session de caisse active ou clôturée.
 * Maintient la stricte séparation des devises USD et CDF.
 */
public class CashSession {

    public enum ClosureState {
        OPEN,
        CHECK_REPORT,
        REPORT_GENERATED,
        REPORT_PRINTED,
        COUNTING,
        RECONCILIATION,
        CONFIRMATION,
        CLOSED
    }

    private int id;
    private CashRegister cashRegister;
    private String cashierUsername;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    // Soldes théoriques calculés par le système
    private double initialBalanceUSD;
    private double initialBalanceCDF;
    private double currentBalanceUSD;
    private double currentBalanceCDF;

    // Comptage physique par la caissière
    private double countedBalanceUSD;
    private double countedBalanceCDF;

    // Écarts calculés
    private double differenceUSD;
    private double differenceCDF;

    // Machine à états de clôture
    private ClosureState closureState;
    private boolean reportGenerated;
    private boolean reportPrinted;
    private String closureNotes;
    private boolean exceptionalClosure;

    public CashSession() {
        this.closureState = ClosureState.OPEN;
        this.openedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public CashRegister getCashRegister() { return cashRegister; }
    public void setCashRegister(CashRegister cashRegister) { this.cashRegister = cashRegister; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public double getInitialBalanceUSD() { return initialBalanceUSD; }
    public void setInitialBalanceUSD(double initialBalanceUSD) { this.initialBalanceUSD = initialBalanceUSD; }

    public double getInitialBalanceCDF() { return initialBalanceCDF; }
    public void setInitialBalanceCDF(double initialBalanceCDF) { this.initialBalanceCDF = initialBalanceCDF; }

    public double getCurrentBalanceUSD() { return currentBalanceUSD; }
    public void setCurrentBalanceUSD(double currentBalanceUSD) { this.currentBalanceUSD = currentBalanceUSD; }

    public double getCurrentBalanceCDF() { return currentBalanceCDF; }
    public void setCurrentBalanceCDF(double currentBalanceCDF) { this.currentBalanceCDF = currentBalanceCDF; }

    public double getCountedBalanceUSD() { return countedBalanceUSD; }
    public void setCountedBalanceUSD(double countedBalanceUSD) { this.countedBalanceUSD = countedBalanceUSD; }

    public double getCountedBalanceCDF() { return countedBalanceCDF; }
    public void setCountedBalanceCDF(double countedBalanceCDF) { this.countedBalanceCDF = countedBalanceCDF; }

    public double getDifferenceUSD() { return differenceUSD; }
    public void setDifferenceUSD(double differenceUSD) { this.differenceUSD = differenceUSD; }

    public double getDifferenceCDF() { return differenceCDF; }
    public void setDifferenceCDF(double differenceCDF) { this.differenceCDF = differenceCDF; }

    public ClosureState getClosureState() { return closureState; }
    public void setClosureState(ClosureState closureState) { this.closureState = closureState; }

    public boolean isReportGenerated() { return reportGenerated; }
    public void setReportGenerated(boolean reportGenerated) { this.reportGenerated = reportGenerated; }

    public boolean isReportPrinted() { return reportPrinted; }
    public void setReportPrinted(boolean reportPrinted) { this.reportPrinted = reportPrinted; }

    public String getClosureNotes() { return closureNotes; }
    public void setClosureNotes(String closureNotes) { this.closureNotes = closureNotes; }

    public boolean isExceptionalClosure() { return exceptionalClosure; }
    public void setExceptionalClosure(boolean exceptionalClosure) { this.exceptionalClosure = exceptionalClosure; }
}
