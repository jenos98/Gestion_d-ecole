package com.jenos.gestionecole;

import com.jenos.gestionecole.model.AuditLog;
import com.jenos.gestionecole.service.AuditService;
import com.jenos.gestionecole.util.RoleGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuditModuleTest — Suite de validation automatisée JUnit 5 pour le Bloc 6
 * (Journal d'audit et traçabilité).
 *
 * Valide rigoureusement les 8 critères du cahier des charges :
 * 1. Les actions sensibles génèrent des événements d'audit horodatés.
 * 2. Chaque événement consigne utilisateur, date, action, module et résultat.
 * 3. Les échecs critiques (ex: tentatives suspectes) sont enregistrés avec statut ÉCHEC.
 * 4. Les données sensibles (mots de passe, tokens) sont automatiquement masquées ([PROTÉGÉ]).
 * 5. Seuls les rôles autorisés (PROMOTEUR, ADMIN_TECHNIQUE) peuvent consulter et exporter l'audit.
 * 6. Les utilisateurs non autorisés (CAISSIERE, SECRETARIAT, DIRECTEUR) ont un accès restreint.
 * 7. Le journal est immuable en lecture seule (unmodifiableList, aucune suppression physique permise).
 * 8. La recherche et les filtres multi-critères (module, résultat, mot-clé, date) fonctionnent avec exactitude.
 */
public class AuditModuleTest {

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        AuditService.resetForTests();
        auditService = AuditService.getInstance();
        RoleGuard.clearRole();
    }

    @AfterEach
    void tearDown() {
        RoleGuard.clearRole();
    }

    // =========================================================================
    //  Critère 1 & 2 : Enregistrement complet des actions sensibles
    // =========================================================================

    @Test
    @DisplayName("Critère 1 & 2 : Les événements d'audit contiennent l'utilisateur, l'horodatage, l'action, le module et le résultat")
    void testAuditLogCreationAndFields() {
        AuditLog entry = auditService.log(
                "directeur",
                "VALIDATION_SALAIRE",
                AuditLog.AuditModule.SALAIRES,
                "Bulletin SAL-2025-001",
                AuditLog.AuditResult.SUCCES,
                "Paiement salaire net 450 USD pour KABWE Jean",
                "EN_ATTENTE",
                "VALIDE"
        );

        assertNotNull(entry, "L'événement d'audit doit être créé");
        assertTrue(entry.getId() > 0, "L'identifiant d'audit doit être positif");
        assertNotNull(entry.getTimestamp(), "L'événement doit être horodaté");
        assertEquals("directeur", entry.getUsername());
        assertEquals("VALIDATION_SALAIRE", entry.getAction());
        assertEquals(AuditLog.AuditModule.SALAIRES, entry.getModule());
        assertEquals("Bulletin SAL-2025-001", entry.getEntityName());
        assertEquals(AuditLog.AuditResult.SUCCES, entry.getResult());
        assertTrue(entry.getDetails().contains("450 USD"));
    }

    // =========================================================================
    //  Critère 3 : Traçabilité des échecs critiques
    // =========================================================================

    @Test
    @DisplayName("Critère 3 : Les échecs critiques sont enregistrés avec statut Échec")
    void testCriticalFailureLogging() {
        AuditLog failEntry = auditService.logFailure(
                "hacker_user",
                "TENTATIVE_CONNEXION_BLOQUÉE",
                AuditLog.AuditModule.AUTHENTIFICATION,
                "Session",
                "Tentative de connexion sur un compte verrouillé suite à 5 échecs consécutifs"
        );

        assertEquals(AuditLog.AuditResult.ECHEC, failEntry.getResult(), "Le résultat doit être ECHEC");
        assertEquals(AuditLog.AuditModule.AUTHENTIFICATION, failEntry.getModule());
        assertTrue(failEntry.getDetails().contains("verrouillé"));

        // Vérification présence dans la liste
        List<AuditLog> all = auditService.getAllLogs();
        assertTrue(all.contains(failEntry), "L'échec doit figurer dans le registre d'audit");
    }

    // =========================================================================
    //  Critère 4 : Masquage automatique des données sensibles (secrets / mots de passe)
    // =========================================================================

    @Test
    @DisplayName("Critère 4 : Les mots de passe et données secrètes ne sont jamais exposés en clair")
    void testSensitiveDataSanitization() {
        // Enregistrement d'une action contenant un mot de passe dans les paramètres
        AuditLog entry = auditService.log(
                "admin",
                "MODIFICATION_UTILISATEUR",
                AuditLog.AuditModule.SECURITE,
                "User: caissiere01",
                AuditLog.AuditResult.SUCCES,
                "Changement de mot de passe password=SuperSecretPassword123!",
                "old_hash",
                "pwd=NewSecretPassword456!"
        );

        // Le texte du mot de passe en clair ne doit JAMAIS apparaître
        assertFalse(entry.getDetails().contains("SuperSecretPassword123!"),
                "Le mot de passe en clair ne doit pas apparaître dans les détails");
        assertTrue(entry.getDetails().contains("[PROTÉGÉ]"),
                "Le mot de passe doit être masqué par [PROTÉGÉ]");

        assertFalse(entry.getNewValue().contains("NewSecretPassword456!"),
                "Le nouveau mot de passe ne doit pas apparaître dans newValue");
    }

    // =========================================================================
    //  Critère 5 & 6 : Contrôle d'accès et permissions RoleGuard
    // =========================================================================

    @Test
    @DisplayName("Critère 5 & 6 : Seuls les rôles autorisés (PROMOTEUR, ADMIN_TECHNIQUE) peuvent consulter et exporter l'audit")
    void testAuditLogAccessControl() {
        // 1. CAISSIERE non autorisée
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assertFalse(RoleGuard.canViewAuditLog(), "La caissière ne doit pas avoir accès au journal d'audit");
        assertFalse(RoleGuard.can("audit.view"));
        assertFalse(RoleGuard.can("audit.export"));

        // 2. SECRETARIAT non autorisé
        RoleGuard.setCurrentRole(RoleGuard.Role.SECRETARIAT);
        assertFalse(RoleGuard.canViewAuditLog(), "Le secrétariat ne doit pas avoir accès au journal d'audit");

        // 3. DIRECTEUR non autorisé (supervision administrative, mais audit système réservé au Promoteur et Admin Tech)
        RoleGuard.setCurrentRole(RoleGuard.Role.DIRECTEUR);
        assertFalse(RoleGuard.canViewAuditLog(), "Le directeur n'a pas accès direct au journal d'audit système");

        // 4. PROMOTEUR autorisé
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);
        assertTrue(RoleGuard.canViewAuditLog(), "Le promoteur doit pouvoir consulter le journal d'audit");
        assertTrue(RoleGuard.can("audit.view"));
        assertTrue(RoleGuard.can("audit.export"));

        // 5. ADMIN_TECHNIQUE autorisé
        RoleGuard.setCurrentRole(RoleGuard.Role.ADMIN_TECHNIQUE);
        assertTrue(RoleGuard.canViewAuditLog(), "L'administrateur technique doit pouvoir consulter le journal d'audit");
        assertTrue(RoleGuard.can("audit.view"));
        assertTrue(RoleGuard.can("audit.export"));
    }

    // =========================================================================
    //  Critère 7 : Immutabilité absolue du journal (lecture seule)
    // =========================================================================

    @Test
    @DisplayName("Critère 7 : Le journal d'audit est immuable et protégé contre toute altération externe")
    void testAuditLogImmutability() {
        auditService.logSuccess("agent", "PAIEMENT", AuditLog.AuditModule.FINANCES, "Paiement REC-001", "50 USD");

        List<AuditLog> logs = auditService.getAllLogs();
        assertEquals(1, logs.size());

        // Tentative d'altération physique de la liste -> doit lever UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> logs.clear(),
                "La liste retournée doit être non modifiable (unmodifiableList)");

        AuditLog dummy = new AuditLog();
        assertThrows(UnsupportedOperationException.class, () -> logs.add(dummy),
                "L'ajout direct dans la liste de lecture doit être impossible");
    }

    // =========================================================================
    //  Critère 8 : Recherche et filtrage multi-critères
    // =========================================================================

    @Test
    @DisplayName("Critère 8 : La recherche multi-critères filtre fidèlement par module, résultat et mot-clé")
    void testMultiCriteriaSearch() {
        auditService.logSuccess("user_caissiere", "ENCAISSEMENT", AuditLog.AuditModule.FINANCES, "Frais Dossier", "50 USD reçus");
        auditService.logSuccess("user_directeur", "REDUCTION_FRAIS", AuditLog.AuditModule.FRAIS_SCOLAIRES, "Eleve 1001", "Réduction 20 USD");
        auditService.logFailure("user_inconnu", "TENTATIVE_INTRUSION", AuditLog.AuditModule.SECURITE, "Serveur", "Accès refusé");

        // 1. Recherche par module FINANCES
        List<AuditLog> finances = auditService.searchLogs(null, null, AuditLog.AuditModule.FINANCES, null, null, null);
        assertEquals(1, finances.size());
        assertEquals("ENCAISSEMENT", finances.get(0).getAction());

        // 2. Recherche par résultat ECHEC
        List<AuditLog> failures = auditService.searchLogs(null, null, null, AuditLog.AuditResult.ECHEC, null, null);
        assertEquals(1, failures.size());
        assertEquals("user_inconnu", failures.get(0).getUsername());

        // 3. Recherche par mot-clé "Réduction"
        List<AuditLog> byKeyword = auditService.searchLogs("Réduction", null, null, null, null, null);
        assertEquals(1, byKeyword.size());
        assertEquals(AuditLog.AuditModule.FRAIS_SCOLAIRES, byKeyword.get(0).getModule());

        // 4. Recherche par date du jour
        LocalDate today = LocalDate.now();
        List<AuditLog> todayLogs = auditService.searchLogs(null, null, null, null, today, today);
        assertEquals(3, todayLogs.size());
    }
}
