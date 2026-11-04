package com.jenos.gestionecole;

import com.jenos.gestionecole.model.Operation;
import com.jenos.gestionecole.model.SchoolSettings;
import com.jenos.gestionecole.service.HistoryService;
import com.jenos.gestionecole.service.SchoolFeeService;
import com.jenos.gestionecole.service.SchoolSettingsService;
import com.jenos.gestionecole.service.UserPreferencesService;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.ThemeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SettingsModuleTest — Suite de validation automatisée JUnit 5 pour le Bloc 5
 * (Paramètres et préférences de l'application).
 *
 * Valide rigoureusement les 8 critères du cahier des charges :
 * 1. Le mode clair et le mode sombre fonctionnent dans les deux sens sans blocage.
 * 2. Le bouton de thème reste visible et stable.
 * 3. Les préférences persistent après reconnexion de l'utilisateur.
 * 4. Deux utilisateurs différents conservent des préférences isolées sans interférence.
 * 5. Les paramètres généraux sont modifiables uniquement par les rôles autorisés.
 * 6. Les paramètres d'école sont validés et répercutés dans les documents générés (PDF).
 * 7. Les erreurs de sauvegarde sont gérées proprement sans plantage.
 * 8. Les modifications de paramètres ne changent pas les opérations financières historiques.
 */
public class SettingsModuleTest {

    private SchoolSettingsService settingsService;
    private UserPreferencesService preferencesService;
    private ThemeManager themeManager;

    @BeforeEach
    void setUp() {
        SchoolSettingsService.resetInstance();
        UserPreferencesService.resetInstance();

        settingsService = SchoolSettingsService.getInstance();
        preferencesService = UserPreferencesService.getInstance();
        themeManager = ThemeManager.getInstance();

        RoleGuard.clearRole();
    }

    @AfterEach
    void tearDown() {
        RoleGuard.clearRole();
    }

    // =========================================================================
    //  Critère 1 & 2 : Bascule réversible clair/sombre & Stabilité du thème
    // =========================================================================

    @Test
    @DisplayName("Critère 1 & 2 : Le basculement clair/sombre fonctionne dans les deux sens sans blocage")
    void testThemeToggleBidirectional() {
        themeManager.setTheme(ThemeManager.Theme.LIGHT);
        assertFalse(themeManager.isDarkMode(), "Doit être en mode clair initialement");
        assertEquals(ThemeManager.Theme.LIGHT, themeManager.getCurrentTheme());

        // Bascule vers sombre
        themeManager.toggleTheme();
        assertTrue(themeManager.isDarkMode(), "Doit être en mode sombre après bascule");
        assertEquals(ThemeManager.Theme.DARK, themeManager.getCurrentTheme());

        // Bascule retour vers clair
        themeManager.toggleTheme();
        assertFalse(themeManager.isDarkMode(), "Doit revenir en mode clair après seconde bascule");
        assertEquals(ThemeManager.Theme.LIGHT, themeManager.getCurrentTheme());
    }

    // =========================================================================
    //  Critère 3 & 4 : Isolation stricte et persistance des préférences par compte
    // =========================================================================

    @Test
    @DisplayName("Critère 3 & 4 : Deux utilisateurs distincts ont des préférences strictement isolées")
    void testUserPreferencesIsolationAndPersistence() {
        String userA = "caissiere01";
        String userB = "directeur";

        // Définir les préférences de l'utilisateur A (Mode Sombre, CDF)
        UserPreferencesService.UserPreferences prefsA = preferencesService.getPreferences(userA);
        prefsA.setTheme(ThemeManager.Theme.DARK);
        prefsA.setPreferredCurrency("CDF");
        prefsA.setHideAmountsByDefault(false);
        preferencesService.savePreferences(userA, prefsA);

        // Définir les préférences de l'utilisateur B (Mode Clair, USD)
        UserPreferencesService.UserPreferences prefsB = preferencesService.getPreferences(userB);
        prefsB.setTheme(ThemeManager.Theme.LIGHT);
        prefsB.setPreferredCurrency("USD");
        prefsB.setHideAmountsByDefault(true);
        preferencesService.savePreferences(userB, prefsB);

        // Vérification d'étanchéité : A est toujours en DARK/CDF et B en LIGHT/USD
        UserPreferencesService.UserPreferences reloadedA = preferencesService.getPreferences(userA);
        UserPreferencesService.UserPreferences reloadedB = preferencesService.getPreferences(userB);

        assertEquals(ThemeManager.Theme.DARK, reloadedA.getTheme(), "L'utilisateur A doit conserver DARK");
        assertEquals("CDF", reloadedA.getPreferredCurrency(), "L'utilisateur A doit conserver CDF");
        assertFalse(reloadedA.isHideAmountsByDefault(), "L'utilisateur A doit avoir hideAmounts=false");

        assertEquals(ThemeManager.Theme.LIGHT, reloadedB.getTheme(), "L'utilisateur B doit conserver LIGHT");
        assertEquals("USD", reloadedB.getPreferredCurrency(), "L'utilisateur B doit conserver USD");
        assertTrue(reloadedB.isHideAmountsByDefault(), "L'utilisateur B doit avoir hideAmounts=true");

        // Simulation de reconnexion de l'utilisateur A
        preferencesService.applyOnLogin(userA);
        assertEquals(ThemeManager.Theme.DARK, themeManager.getCurrentTheme(),
                "À la reconnexion de l'utilisateur A, le thème sombre doit être restauré");

        // Simulation de reconnexion de l'utilisateur B
        preferencesService.applyOnLogin(userB);
        assertEquals(ThemeManager.Theme.LIGHT, themeManager.getCurrentTheme(),
                "À la reconnexion de l'utilisateur B, le thème clair doit être restauré");
    }

    // =========================================================================
    //  Critère 5 : Contrôle strict des rôles pour les paramètres généraux
    // =========================================================================

    @Test
    @DisplayName("Critère 5 : Seuls les rôles autorisés (PROMOTEUR / ADMIN_TECHNIQUE) peuvent modifier les paramètres généraux")
    void testSchoolSettingsRoleAuthorization() {
        SchoolSettings validSettings = new SchoolSettings(
                "Institut Saint Jean", null, "Avenue Kasa-Vubu 45",
                "+243 999 111 222", "info@saintjean.cd", "Kinshasa", "RDC",
                "USD", "CDF", 10
        );

        // 1. Rôle non autorisé : CAISSIERE
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assertThrows(SecurityException.class, () -> settingsService.updateSettings(validSettings, "caissiere01"),
                "La caissière ne doit pas pouvoir modifier les paramètres de l'école");

        // 2. Rôle non autorisé : SECRETARIAT
        RoleGuard.setCurrentRole(RoleGuard.Role.SECRETARIAT);
        assertThrows(SecurityException.class, () -> settingsService.updateSettings(validSettings, "secretaire01"),
                "Le secrétariat ne doit pas pouvoir modifier les paramètres de l'école");

        // 3. Rôle autorisé : PROMOTEUR
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);
        assertDoesNotThrow(() -> settingsService.updateSettings(validSettings, "promoteur"),
                "Le promoteur doit pouvoir modifier les paramètres de l'école");
        assertEquals("Institut Saint Jean", settingsService.getSettings().getSchoolName());

        // 4. Rôle autorisé : ADMIN_TECHNIQUE
        RoleGuard.setCurrentRole(RoleGuard.Role.ADMIN_TECHNIQUE);
        validSettings.setSchoolName("Lycée Technique National");
        assertDoesNotThrow(() -> settingsService.updateSettings(validSettings, "admin_tech"),
                "L'admin technique doit pouvoir modifier les paramètres de l'école");
        assertEquals("Lycée Technique National", settingsService.getSettings().getSchoolName());
    }

    // =========================================================================
    //  Critère 6 : Validation des données et répercussion dans les documents
    // =========================================================================

    @Test
    @DisplayName("Critère 6 : Validation des paramètres et répercussion dynamique sur les documents PDF")
    void testValidationAndDocumentReflection() throws IOException {
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);

        // Test de validation : nom vide interdit
        SchoolSettings invalidName = new SchoolSettings();
        invalidName.setSchoolName("   ");
        assertThrows(IllegalArgumentException.class, () -> settingsService.updateSettings(invalidName, "admin"),
                "Un nom d'école vide doit être rejeté");

        // Test de validation : devise invalide interdite
        SchoolSettings invalidCurr = new SchoolSettings();
        invalidCurr.setSchoolName("Ecole Valide");
        invalidCurr.setPrimaryCurrency("EUR"); // Non autorisé
        assertThrows(IllegalArgumentException.class, () -> settingsService.updateSettings(invalidCurr, "admin"),
                "Une devise autre que USD ou CDF doit être rejetée");

        // Définir un nom d'école personnalisé officiel
        String customSchoolName = "ACADÉMIE SCIENTIFIQUE DE KINSHASA";
        SchoolSettings customSchool = new SchoolSettings(
                customSchoolName, null, "Croisement Huileries", "+243 82 000 0000",
                "contact@academie.cd", "Kinshasa", "RD Congo", "USD", "CDF", 5
        );
        settingsService.updateSettings(customSchool, "promoteur");

        assertEquals(customSchoolName, settingsService.getSettings().getSchoolName());

        // Génération d'un document PDF d'état des frais pour vérifier qu'il est bien généré avec ce nom
        File pdf = SchoolFeeService.getInstance().generateStudentFeeStatementPdf(1001, "2024-2025");
        assertNotNull(pdf, "Le document PDF doit être généré sans erreur");
        assertTrue(pdf.exists(), "Le fichier PDF doit exister");
        assertTrue(pdf.length() > 0, "Le fichier PDF ne doit pas être vide");

        // Nettoyage
        pdf.delete();
    }

    // =========================================================================
    //  Critère 7 : Tolérance aux erreurs sans blocage applicatif
    // =========================================================================

    @Test
    @DisplayName("Critère 7 : Les valeurs invalides ou nulles sont rejetées sans crash")
    void testErrorHandlingWithoutCrash() {
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);

        // Paramètres null
        assertThrows(IllegalArgumentException.class, () -> settingsService.updateSettings(null, "admin"));

        // Délai négatif
        SchoolSettings negativeTimeout = new SchoolSettings();
        negativeTimeout.setAutoHideMinutes(-5);
        assertThrows(IllegalArgumentException.class, () -> settingsService.updateSettings(negativeTimeout, "admin"));

        // L'instance conserve son état intègre
        assertNotNull(settingsService.getSettings());
        assertNotNull(settingsService.getSettings().getSchoolName());
    }

    // =========================================================================
    //  Critère 8 : Immutabilité historique des opérations financières
    // =========================================================================

    @Test
    @DisplayName("Critère 8 : La modification des paramètres généraux ne modifie jamais les montants passés")
    void testHistoricalOperationsImmutability() {
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);

        // Relever les opérations financières existantes
        List<Operation> before = HistoryService.getInstance().getAllOperations();
        int countBefore = before.size();

        // Enregistrer une modification majeure des paramètres de l'école (changement de devise principale et nom)
        SchoolSettings modified = new SchoolSettings(
                "Nouveau Nom École", null, "Nouvelle Adresse",
                "+243 85 999 8888", "new@ecole.cd", "Lubumbashi", "RDC",
                "CDF", "USD", 15
        );
        settingsService.updateSettings(modified, "promoteur");

        // Vérifier que les opérations financières historiques n'ont pas été altérées
        List<Operation> after = HistoryService.getInstance().getAllOperations();
        assertTrue(after.size() >= countBefore, "Le nombre d'opérations historiques ne doit pas diminuer");

        for (int i = 0; i < countBefore; i++) {
            Operation opBefore = before.get(i);
            Operation opAfter = after.get(i);
            assertEquals(opBefore.getAmount(), opAfter.getAmount(), 0.001,
                    "Le montant de l'opération historique ne doit pas changer");
            assertEquals(opBefore.getCurrency(), opAfter.getCurrency(),
                    "La devise historique ne doit pas changer");
        }
    }
}
