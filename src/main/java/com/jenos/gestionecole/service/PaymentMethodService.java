package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.PaymentMethodConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * PaymentMethodService — Gestion des modes de paiement configurés.
 */
public class PaymentMethodService {

    private static PaymentMethodService instance;
    private final List<PaymentMethodConfig> methods = new ArrayList<>();

    private PaymentMethodService() {
        initMockMethods();
    }

    public static PaymentMethodService getInstance() {
        if (instance == null) {
            instance = new PaymentMethodService();
        }
        return instance;
    }

    private void initMockMethods() {
        methods.add(new PaymentMethodConfig(1, "ESPECES", "Espèces au guichet", false, true));
        methods.add(new PaymentMethodConfig(2, "MOBILE_MONEY", "Mobile Money (M-Pesa, Orange, Airtel)", true, true));
        methods.add(new PaymentMethodConfig(3, "VIREMENT", "Virement Bancaire", true, true));
        methods.add(new PaymentMethodConfig(4, "CHEQUE", "Chèque Bancaire", true, true));
    }

    public List<PaymentMethodConfig> getAllMethods() {
        return new ArrayList<>(methods);
    }

    public List<PaymentMethodConfig> getActiveMethods() {
        List<PaymentMethodConfig> active = new ArrayList<>();
        for (PaymentMethodConfig m : methods) {
            if (m.isActive()) active.add(m);
        }
        return active;
    }

    public PaymentMethodConfig findByCode(String code) {
        if (code == null) return null;
        for (PaymentMethodConfig m : methods) {
            if (code.equalsIgnoreCase(m.getCode())) return m;
        }
        return null;
    }

    public void addMethod(String code, String name, boolean requiresReference) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Le code est obligatoire.");
        if (findByCode(code) != null) throw new IllegalArgumentException("Le mode " + code + " existe déjà.");

        PaymentMethodConfig m = new PaymentMethodConfig(methods.size() + 1, code.toUpperCase().trim(), name, requiresReference, true);
        methods.add(m);
        AuditService.getInstance().logAction("admin", "AJOUT_MODE_PAIEMENT", "PaymentMethod", null, code);
    }

    public void toggleActive(int id) {
        for (PaymentMethodConfig m : methods) {
            if (m.getId() == id) {
                m.setActive(!m.isActive());
                AuditService.getInstance().logAction("admin", "STATUT_MODE_PAIEMENT", "PaymentMethod", m.getCode(), "Actif=" + m.isActive());
                return;
            }
        }
    }
}
