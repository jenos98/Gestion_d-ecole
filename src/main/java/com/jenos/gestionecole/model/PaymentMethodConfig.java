package com.jenos.gestionecole.model;

/**
 * PaymentMethodConfig — Configuration d'un mode de paiement supporté.
 * Exemples : Espèces, Virement bancaire, Mobile Money, Chèque, etc.
 */
public class PaymentMethodConfig {

    private int id;
    private String code;            // ex: "ESPECES", "MOBILE_MONEY", "VIREMENT", "CHEQUE"
    private String name;            // ex: "Espèces au guichet", "Mobile Money (M-Pesa, Airtel)"
    private boolean requiresReference; // ex: OUI pour Mobile Money ou Chèque
    private boolean active;

    public PaymentMethodConfig() {
        this.active = true;
    }

    public PaymentMethodConfig(int id, String code, String name, boolean requiresReference, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.requiresReference = requiresReference;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isRequiresReference() { return requiresReference; }
    public void setRequiresReference(boolean requiresReference) { this.requiresReference = requiresReference; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return name + (requiresReference ? " [Réf. requise]" : "");
    }
}
