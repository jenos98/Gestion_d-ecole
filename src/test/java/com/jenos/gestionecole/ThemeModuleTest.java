package com.jenos.gestionecole;

import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.service.SessionManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.ThemeManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThemeModuleTest — Validation automatisée du Bloc 1 (Mode Sombre / Mode Clair).
 * Couvre l'ensemble des 8 critères de validation de la checklist.
 */
public class ThemeModuleTest {

    @BeforeAll
    public static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // JavaFX déjà initialisé
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Le toolkit JavaFX doit être initialisé");
    }

    @BeforeEach
    public void setUp() {
        ThemeManager.getInstance().resetForTests();
        SessionManager.getInstance().resetForTests();
    }

    @Test
    @DisplayName("Critère 1 & 2 : Bascule immédiate Clair -> Sombre -> Clair en un clic")
    void testToggleThemeInOneClick() {
        ThemeManager tm = ThemeManager.getInstance();
        assertFalse(tm.isDarkMode(), "Le thème initial doit être CLAIR");
        assertEquals(ThemeManager.Theme.LIGHT, tm.getCurrentTheme());

        // 1er clic : activation mode sombre
        tm.toggleTheme();
        assertTrue(tm.isDarkMode(), "Après 1 clic, le mode sombre doit être ACTIF");
        assertEquals(ThemeManager.Theme.DARK, tm.getCurrentTheme());

        // 2ème clic : réactivation mode clair
        tm.toggleTheme();
        assertFalse(tm.isDarkMode(), "Après 2ème clic, le mode clair doit être RÉACTIVÉ");
        assertEquals(ThemeManager.Theme.LIGHT, tm.getCurrentTheme());
    }

    @Test
    @DisplayName("Critère 3 & 4 : Application dynamique de la feuille theme-dark.css à la scène")
    void testThemeStylesheetAppliedToScene() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean error = new AtomicBoolean(false);

        Platform.runLater(() -> {
            try {
                ThemeManager tm = ThemeManager.getInstance();
                Scene testScene = new Scene(new StackPane(), 800, 600);
                tm.applyTheme(testScene);

                // Initialement clair : theme-dark.css ne doit PAS être présent
                boolean containsDark = testScene.getStylesheets().stream().anyMatch(s -> s.contains("theme-dark.css"));
                assertFalse(containsDark, "Le mode clair ne doit pas inclure theme-dark.css");

                // Bascule vers sombre : theme-dark.css DOIT être injecté
                tm.setTheme(ThemeManager.Theme.DARK);
                containsDark = testScene.getStylesheets().stream().anyMatch(s -> s.contains("theme-dark.css"));
                assertTrue(containsDark, "Le mode sombre doit injecter theme-dark.css");

                // Bascule retour vers clair : theme-dark.css DOIT être retiré
                tm.setTheme(ThemeManager.Theme.LIGHT);
                containsDark = testScene.getStylesheets().stream().anyMatch(s -> s.contains("theme-dark.css"));
                assertFalse(containsDark, "Le retour au mode clair doit retirer theme-dark.css");
            } catch (Throwable t) {
                error.set(true);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertFalse(error.get(), "L'application des feuilles CSS sur la scène a échoué");
    }

    @Test
    @DisplayName("Critère 5 & 6 : Préférence enregistrée et restaurée par utilisateur après reconnexion")
    void testUserPreferencePersistedAndRestored() {
        ThemeManager tm = ThemeManager.getInstance();
        String userA = "caissiere01";

        // Connexion utilisateur A
        tm.onUserLogin(userA);
        tm.setTheme(ThemeManager.Theme.DARK);
        assertTrue(tm.isDarkMode());

        // Déconnexion : retour au thème clair pour l'écran de login
        tm.onUserLogout();
        assertFalse(tm.isDarkMode(), "L'écran de déconnexion/login doit revenir au thème neutre CLAIR");

        // Reconnexion de utilisateur A : restauration automatique de son mode sombre
        tm.onUserLogin(userA);
        assertTrue(tm.isDarkMode(), "La préférence sombre de l'utilisateur doit être restaurée");
    }

    @Test
    @DisplayName("Critère 7 : Deux utilisateurs distincts conservent des thèmes indépendants")
    void testTwoUsersHaveIndependentThemes() {
        ThemeManager tm = ThemeManager.getInstance();
        String userSombre = "admin_promoteur";
        String userClair = "directeur";

        // Utilisateur 1 préfère le sombre
        tm.onUserLogin(userSombre);
        tm.setTheme(ThemeManager.Theme.DARK);
        assertTrue(tm.isDarkMode());
        tm.onUserLogout();

        // Utilisateur 2 préfère le clair
        tm.onUserLogin(userClair);
        tm.setTheme(ThemeManager.Theme.LIGHT);
        assertFalse(tm.isDarkMode());
        tm.onUserLogout();

        // Vérification de non-contamination :
        // 1. Reconnexion User 1 -> doit être SOMBRE
        tm.onUserLogin(userSombre);
        assertTrue(tm.isDarkMode(), "User 1 doit retrouver son thème sombre");
        tm.onUserLogout();

        // 2. Reconnexion User 2 -> doit être CLAIR
        tm.onUserLogin(userClair);
        assertFalse(tm.isDarkMode(), "User 2 doit retrouver son thème clair sans subir le thème de User 1");
    }

    @Test
    @DisplayName("Critère 8 : Tolérance aux pannes & immunité des données métier")
    void testRobustnessAndBusinessDataImmunity() {
        ThemeManager tm = ThemeManager.getInstance();

        // 1. Passage d'identifiants nuls ou invalides sans plantage
        assertDoesNotThrow(() -> tm.onUserLogin(null));
        assertDoesNotThrow(() -> tm.onUserLogin(""));
        assertDoesNotThrow(() -> tm.setTheme(null));
        assertDoesNotThrow(tm::toggleTheme);

        // 2. Vérification que RoleGuard et les permissions restent intactes
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assertTrue(RoleGuard.can("payment.create"));
        assertFalse(RoleGuard.canManageSettings());

        // Bascule de thème
        tm.setTheme(ThemeManager.Theme.DARK);

        // Les droits ne sont en aucun cas altérés
        assertTrue(RoleGuard.can("payment.create"), "Les droits de caisse doivent rester inchangés après bascule");
        assertFalse(RoleGuard.canManageSettings(), "Les restrictions de sécurité doivent rester inchangées");
    }
}
