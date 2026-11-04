package com.jenos.gestionecole.model;

/**
 * CashRegister — Caisse de l'établissement.
 */
public class CashRegister {

    public enum Status {
        OPEN,
        CLOSED
    }

    private int id;
    private String name;        // ex: "Caisse 01", "Caisse Principale"
    private String code;        // ex: "CS-01"
    private String currentCashier;
    private Status status;

    public CashRegister() {
        this.status = Status.CLOSED;
    }

    public CashRegister(int id, String name, String code, String currentCashier, Status status) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.currentCashier = currentCashier;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCurrentCashier() { return currentCashier; }
    public void setCurrentCashier(String currentCashier) { this.currentCashier = currentCashier; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Override
    public String toString() {
        return name + " (" + status + ")";
    }
}
