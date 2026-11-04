package com.jenos.gestionecole.service;

import com.jenos.gestionecole.util.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

/**
 * UserPreferencesService — Gestionnaire centralisé des préférences individuelles par compte utilisateur.
 *
 * RÈGLES DU BLOC 5 :
 * 1. Isolation stricte : les préférences d'un utilisateur n'impactent jamais celles des autres comptes.
 * 2. Persistance fiable par compte (nom d'utilisateur).
 * 3. Tolérance aux pannes : en cas d'erreur de stockage système, un cache mémoire assure la continuité sans crash.
 * 4. Restauration automatique lors de la connexion.
 */
public class UserPreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(UserPreferencesService.class);
    private static UserPreferencesService instance;

    private static final String PREF_NODE = "cd.ecole.gestion.preferences";
    private static final String KEY_THEME = "theme_";
    private static final String KEY_CURRENCY = "preferred_currency_";
    private static final String KEY_HIDE_MONEY = "hide_money_";

    public static class UserPreferences {
        private String username;
        private ThemeManager.Theme theme = ThemeManager.Theme.LIGHT;
        private String preferredCurrency = "USD";
        private boolean hideAmountsByDefault = true;

        public UserPreferences(String username) {
            this.username = username != null ? username : "default";
        }

        public String getUsername() { return username; }
        public ThemeManager.Theme getTheme() { return theme; }
        public void setTheme(ThemeManager.Theme theme) { this.theme = theme != null ? theme : ThemeManager.Theme.LIGHT; }

        public String getPreferredCurrency() { return preferredCurrency; }
        public void setPreferredCurrency(String preferredCurrency) {
            if ("CDF".equalsIgnoreCase(preferredCurrency) || "USD".equalsIgnoreCase(preferredCurrency)) {
                this.preferredCurrency = preferredCurrency.toUpperCase();
            }
        }

        public boolean isHideAmountsByDefault() { return hideAmountsByDefault; }
        public void setHideAmountsByDefault(boolean hideAmountsByDefault) { this.hideAmountsByDefault = hideAmountsByDefault; }
    }

    // Cache mémoire pour garantir l'isolation et la continuité en cas de panne de Preferences
    private final Map<String, UserPreferences> memoryCache = new ConcurrentHashMap<>();

    private UserPreferencesService() {}

    public static synchronized UserPreferencesService getInstance() {
        if (instance == null) {
            instance = new UserPreferencesService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    /**
     * Récupère les préférences associées à un utilisateur spécifique.
     * Si l'utilisateur n'en a pas encore, les valeurs par défaut sont initialisées.
     */
    public UserPreferences getPreferences(String username) {
        String key = (username != null && !username.isBlank()) ? username.trim().toLowerCase() : "default";

        return memoryCache.computeIfAbsent(key, u -> {
            UserPreferences prefs = new UserPreferences(u);
            try {
                Preferences p = Preferences.userRoot().node(PREF_NODE);
                String themeStr = p.get(KEY_THEME + u, "LIGHT");
                prefs.setTheme(ThemeManager.Theme.valueOf(themeStr));
                prefs.setPreferredCurrency(p.get(KEY_CURRENCY + u, "USD"));
                prefs.setHideAmountsByDefault(p.getBoolean(KEY_HIDE_MONEY + u, true));
            } catch (Exception e) {
                logger.warn("Impossible de charger les préférences système pour '{}', utilisation des valeurs par défaut", u);
            }
            return prefs;
        });
    }

    /**
     * Enregistre les préférences pour un utilisateur donné de manière isolée et sécurisée.
     */
    public void savePreferences(String username, UserPreferences prefs) {
        if (prefs == null) return;
        String key = (username != null && !username.isBlank()) ? username.trim().toLowerCase() : "default";

        memoryCache.put(key, prefs);

        try {
            Preferences p = Preferences.userRoot().node(PREF_NODE);
            p.put(KEY_THEME + key, prefs.getTheme().name());
            p.put(KEY_CURRENCY + key, prefs.getPreferredCurrency());
            p.putBoolean(KEY_HIDE_MONEY + key, prefs.isHideAmountsByDefault());
            p.flush();
            logger.info("Préférences enregistrées avec succès pour l'utilisateur '{}'", key);
        } catch (Exception e) {
            logger.warn("Échec d'enregistrement persistant des préférences pour '{}', conservation en mémoire", key, e);
        }

        // Synchroniser avec ThemeManager si l'utilisateur actif est celui modifié
        if (username != null && username.equalsIgnoreCase(ThemeManager.getInstance().getCurrentUsername())) {
            ThemeManager.getInstance().setTheme(prefs.getTheme());
        }
    }

    /**
     * Applique immédiatement les préférences d'un utilisateur lors de sa connexion.
     */
    public void applyOnLogin(String username) {
        UserPreferences prefs = getPreferences(username);
        ThemeManager.getInstance().onUserLogin(username);
        ThemeManager.getInstance().setTheme(prefs.getTheme());
        logger.info("Préférences appliquées à la connexion de '{}' : Thème={}", username, prefs.getTheme());
    }
}
