package com.jenos.gestionecole.util;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

/**
 * ThemeManager — Gestionnaire centralisé du mode clair / mode sombre (Singleton).
 *
 * Règles :
 * 1. Application immédiate du thème sur toute l'interface.
 * 2. Préférences enregistrées individuellement par utilisateur connecté.
 * 3. Isolation stricte entre utilisateurs (logout réinitialise vers le thème clair par défaut).
 * 4. Tolérance aux pannes : échec d'enregistrement géré sans bloquer l'UI.
 * 5. Aucune interférence avec les données métier ou les droits d'accès.
 */
public class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    private static final String CSS_APP_PATH = "/css/app.css";
    private static final String CSS_DARK_PATH = "/css/theme-dark.css";
    private static final String PREF_KEY_PREFIX = "user_theme_";

    public enum Theme {
        LIGHT,
        DARK
    }

    @FunctionalInterface
    public interface ThemeChangeListener {
        void onThemeChanged(Theme newTheme);
    }

    private static ThemeManager instance;

    private Theme currentTheme = Theme.LIGHT;
    private String currentUsername = null;

    // Registre faible des scènes pour propager le thème à tous les dialogues et fenêtres actives
    private final Set<Scene> activeScenes = Collections.newSetFromMap(new WeakHashMap<>());
    private final List<ThemeChangeListener> listeners = new CopyOnWriteArrayList<>();

    private ThemeManager() {}

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTION DU THÈME
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    /**
     * Bascule entre le mode clair et le mode sombre.
     */
    public void toggleTheme() {
        setTheme(isDarkMode() ? Theme.LIGHT : Theme.DARK);
    }

    /**
     * Applique un thème spécifique.
     */
    public void setTheme(Theme theme) {
        if (theme == null) theme = Theme.LIGHT;
        this.currentTheme = theme;

        // Sauvegarde de la préférence utilisateur courante
        saveUserPreference(currentUsername, currentTheme);

        // Application sur toutes les scènes actives
        runOnFx(() -> {
            for (Scene scene : activeScenes) {
                if (scene != null) {
                    applyThemeToScene(scene);
                }
            }
            // Scène principale via NavigationManager si disponible
            Stage stage = NavigationManager.getInstance().getPrimaryStage();
            if (stage != null && stage.getScene() != null) {
                applyThemeToScene(stage.getScene());
            }

            // Notification des écouteurs UI
            notifyListeners();
        });
    }

    /**
     * Applique le thème courant à une scène donnée et l'enregistre dans le registre actif.
     * Indispensable pour les dialogues créés par DialogManager.
     */
    public void applyTheme(Scene scene) {
        if (scene == null) return;
        activeScenes.add(scene);
        applyThemeToScene(scene);
    }

    private void applyThemeToScene(Scene scene) {
        if (scene == null) return;
        try {
            // S'assurer que app.css est présent
            URL appCssUrl = getClass().getResource(CSS_APP_PATH);
            if (appCssUrl != null) {
                String appCss = appCssUrl.toExternalForm();
                if (!scene.getStylesheets().contains(appCss)) {
                    scene.getStylesheets().add(appCss);
                }
            }

            // Gestion de theme-dark.css
            URL darkCssUrl = getClass().getResource(CSS_DARK_PATH);
            if (darkCssUrl != null) {
                String darkCss = darkCssUrl.toExternalForm();
                if (currentTheme == Theme.DARK) {
                    if (!scene.getStylesheets().contains(darkCss)) {
                        scene.getStylesheets().add(darkCss);
                    }
                } else {
                    scene.getStylesheets().remove(darkCss);
                }
            }
        } catch (Exception e) {
            logger.warn("Erreur lors de l'application de la feuille de style à la scène : {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SESSION UTILISATEUR & PERSISTANCE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Appelé lors de la connexion réussie d'un utilisateur.
     * Charge et applique sa préférence personnelle.
     */
    public void onUserLogin(String username) {
        this.currentUsername = (username != null && !username.isBlank()) ? username.trim().toLowerCase() : null;
        Theme savedTheme = loadUserPreference(currentUsername);
        logger.info("Restauration du thème pour '{}' : {}", currentUsername, savedTheme);
        setTheme(savedTheme);
    }

    /**
     * Appelé lors de la déconnexion de l'utilisateur.
     * Réinitialise le thème à LIGHT pour l'écran de connexion sans effacer la préférence enregistrée.
     */
    public void onUserLogout() {
        this.currentUsername = null;
        logger.info("Déconnexion : réinitialisation du thème vers LIGHT.");
        setTheme(Theme.LIGHT);
    }

    private void saveUserPreference(String username, Theme theme) {
        if (username == null || username.isBlank() || theme == null) return;
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            prefs.put(PREF_KEY_PREFIX + username.toLowerCase(), theme.name());
            prefs.flush();
        } catch (Throwable t) {
            // Règle 7 : Si l'enregistrement échoue, gérer sans bloquer l'interface
            logger.warn("Échec non bloquant de l'enregistrement de la préférence de thème pour '{}' : {}", username, t.getMessage());
        }
    }

    private Theme loadUserPreference(String username) {
        if (username == null || username.isBlank()) return Theme.LIGHT;
        try {
            Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
            String val = prefs.get(PREF_KEY_PREFIX + username.toLowerCase(), Theme.LIGHT.name());
            return Theme.valueOf(val);
        } catch (Throwable t) {
            logger.warn("Échec non bloquant de la lecture de la préférence de thème pour '{}' : {}", username, t.getMessage());
            return Theme.LIGHT;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÉCOUTEURS
    // ─────────────────────────────────────────────────────────────────────────

    public void addListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ThemeChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (ThemeChangeListener listener : listeners) {
            try {
                listener.onThemeChanged(currentTheme);
            } catch (Exception e) {
                logger.warn("Erreur dans l'écouteur de thème : {}", e.getMessage());
            }
        }
    }

    private void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            try {
                Platform.runLater(r);
            } catch (IllegalStateException e) {
                // Environnement headless / tests unitaires sans Toolkit
                r.run();
            }
        }
    }

    /**
     * Méthode utilitaire de réinitialisation pour les tests.
     */
    public void resetForTests() {
        this.currentTheme = Theme.LIGHT;
        this.currentUsername = null;
        this.activeScenes.clear();
        this.listeners.clear();
    }
}
