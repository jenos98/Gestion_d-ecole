package com.jenos.gestionecole.util;

import com.jenos.gestionecole.util.MoneyVisibilityManager.VisibilityObserver;

/**
 * MoneyFormatter - Utilitaire de formatage et masquage des montants.
 *
 * Règles :
 * - CDF et USD ne sont JAMAIS additionnés ni convertis silencieusement.
 * - Les montants masqués sont affichés "••••••".
 * - Le formatage est cohérent dans toute l'application.
 */
public class MoneyFormatter {

    private static final String MASK = "••••••";

    private MoneyFormatter() {}

    // -------------------------------------------------------------------------
    // FORMATAGE
    // -------------------------------------------------------------------------

    /** Formate un montant en CDF. Exemple : "12 500 CDF" */
    public static String formatCDF(double amount) {
        return String.format("%,.0f CDF", amount);
    }

    /** Formate un montant en USD. Exemple : "125.00 USD" */
    public static String formatUSD(double amount) {
        return String.format("%.2f USD", amount);
    }

    /**
     * Formate selon la devise.
     * @param amount montant
     * @param currency "CDF" ou "USD"
     */
    public static String format(double amount, String currency) {
        if ("USD".equalsIgnoreCase(currency)) {
            return formatUSD(amount);
        } else if ("CDF".equalsIgnoreCase(currency)) {
            return formatCDF(amount);
        }
        return String.format("%.2f %s", amount, currency);
    }

    // -------------------------------------------------------------------------
    // MASQUAGE
    // -------------------------------------------------------------------------

    /** Retourne le masque "••••••" ou le montant selon l'état du manager. */
    public static String display(double amount, String currency) {
        if (MoneyVisibilityManager.getInstance().isVisible()) {
            return format(amount, currency);
        }
        return MASK;
    }

    /** Retourne le masque statique. */
    public static String getMask() {
        return MASK;
    }

    public static String formatMasked() {
        return MASK;
    }

    public static String format(String formattedText) {
        return formattedText != null ? formattedText : "—";
    }
}
