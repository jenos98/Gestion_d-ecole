package com.jenos.gestionecole.model;

/**
 * Budget — Ligne budgétaire de l'établissement.
 */
public class Budget {

    private int id;
    private String name;
    private String code;
    private double allocatedAmount;
    private double spentAmount;
    private String currency; // "USD" ou "CDF"
    private String category;
    private String status; // "ACTIF", "ARCHIVE", "DEPASSE"
    private String fiscalYear;

    public Budget() {
        this.status = "ACTIF";
        this.category = "Fonctionnement";
    }

    public Budget(int id, String name, String code, double allocatedAmount, double spentAmount, String currency, String fiscalYear) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.allocatedAmount = allocatedAmount;
        this.spentAmount = spentAmount;
        this.currency = currency;
        this.fiscalYear = fiscalYear;
        this.category = "Fonctionnement";
        this.status = (spentAmount > allocatedAmount) ? "DEPASSE" : "ACTIF";
    }

    public Budget(int id, String name, String code, String category, double allocatedAmount, double spentAmount, String currency, String fiscalYear, String status) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.category = category;
        this.allocatedAmount = allocatedAmount;
        this.spentAmount = spentAmount;
        this.currency = currency;
        this.fiscalYear = fiscalYear;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCategory() { return category != null ? category : "Général"; }
    public void setCategory(String category) { this.category = category; }

    public double getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(double allocatedAmount) { this.allocatedAmount = allocatedAmount; }

    public double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }

    public double getAvailableAmount() {
        return Math.max(0, allocatedAmount - spentAmount);
    }

    public double getConsumptionRate() {
        if (allocatedAmount <= 0) return 0.0;
        return (spentAmount / allocatedAmount) * 100.0;
    }

    public boolean isWarning80() {
        return getConsumptionRate() >= 80.0 && getConsumptionRate() < 90.0;
    }

    public boolean isWarning90() {
        return getConsumptionRate() >= 90.0 && getConsumptionRate() < 100.0;
    }

    public boolean isExceeded100() {
        return getConsumptionRate() >= 100.0;
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }

    public String getStatus() { return status != null ? status : "ACTIF"; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return name + " (" + getAvailableAmount() + " " + currency + " dispo)";
    }
}
