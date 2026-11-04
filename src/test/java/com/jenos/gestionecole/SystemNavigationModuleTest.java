package com.jenos.gestionecole;

import com.jenos.gestionecole.model.AuditLog;
import com.jenos.gestionecole.model.SystemNotification;
import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.service.AuditService;
import com.jenos.gestionecole.service.NotificationService;
import com.jenos.gestionecole.service.SessionManager;
import com.jenos.gestionecole.service.UserService;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;

import java.util.List;

/**
 * SystemNavigationModuleTest — Suite de validation automatisée du Bloc 7 :
 * 1. Authentification & initialisation de session (SessionManager)
 * 2. Gestion des échecs, neutralité du message et verrouillage après 5 tentatives
 * 3. Blocage immédiat des comptes BLOQUE et DESACTIVE
 * 4. Cloisonnement strict du dashboard et isolation financière par rôle (RoleGuard)
 * 5. Gestion des alertes, priorités et persistance des notifications critiques (NotificationService)
 * 6. Sécurité du profil et règles strictes de changement de mot de passe
 * 7. Immuabilité et traçabilité du journal d'audit (AuditService)
 * 8. Déconnexion et réinitialisation de l'environnement de sécurité
 */
public class SystemNavigationModuleTest {

    public static void main(String[] args) {
        System.out.println("=== DÉBUT DES TESTS BLOC 7 (Système, Sécurité UI, Dashboard & Navigation) ===");

        testAuthenticationSuccessAndSessionInit();
        testAuthenticationFailuresAndLockout();
        testBlockedAndDeactivatedAccounts();
        testRoleTailoredFinancialIsolation();
        testNotificationServiceOperations();
        testProfilePasswordUpdateSecurity();
        testAuditLogImmutability();
        testLogoutFlow();

        System.out.println("=== TOUS LES TESTS DU BLOC 7 ONT RÉUSSI (100%) ===");
    }

    private static void testAuthenticationSuccessAndSessionInit() {
        System.out.print("[TEST 1] Authentification réussie & initialisation de session... ");
        SessionManager session = SessionManager.getInstance();
        session.resetForTests();

        User user = session.login("admin_promoteur", "Admin1234!");
        assert user != null : "L'utilisateur doit être retourné";
        assert session.isLoggedIn() : "La session doit être active";
        assert "admin_promoteur".equalsIgnoreCase(session.getCurrentUser().getUsername()) : "L'utilisateur connecté doit correspondre";
        assert RoleGuard.getCurrentRole() == RoleGuard.Role.PROMOTEUR : "Le rôle dans RoleGuard doit être PROMOTEUR";
        assert session.getActiveAcademicYear() != null : "L'année académique active doit être chargée dans la session";

        // Vérification de la trace d'audit
        List<AuditLog> logs = AuditService.getInstance().getAllLogs();
        boolean foundLoginAudit = logs.stream().anyMatch(l -> "CONNEXION".equals(l.getAction()) && "admin_promoteur".equals(l.getUsername()));
        assert foundLoginAudit : "Un log d'audit de connexion doit être enregistré";

        System.out.println("OK");
    }

    private static void testAuthenticationFailuresAndLockout() {
        System.out.print("[TEST 2] Échecs d'authentification & verrouillage automatique après 5 tentatives... ");
        SessionManager session = SessionManager.getInstance();
        session.resetForTests();

        // 1. Mot de passe incorrect : message générique obligatoire
        try {
            session.login("caissiere01", "MauvaisMotDePasse!");
            assert false : "La connexion avec mauvais mot de passe aurait dû échouer";
        } catch (SecurityException e) {
            assert "Identifiant ou mot de passe incorrect.".equals(e.getMessage()) : "Message neutre requis pour la sécurité";
        }

        assert session.getFailedAttempts("caissiere01") == 1 : "Le compteur d'échecs doit valoir 1";

        // 2. Répétition jusqu'à 5 échecs consécutifs
        for (int i = 2; i <= 5; i++) {
            try {
                session.login("caissiere01", "Mauvais" + i);
            } catch (SecurityException ignored) {}
        }

        assert session.isAccountLocked("caissiere01") : "Le compte doit être temporairement verrouillé après 5 échecs";

        // 3. La 6ème tentative doit être immédiatement rejetée pour verrouillage
        try {
            session.login("caissiere01", "Caisse1234!");
            assert false : "La connexion doit être bloquée pendant le verrouillage même avec le bon mot de passe";
        } catch (SecurityException e) {
            assert e.getMessage().contains("Trop de tentatives infructueuses") : "Message de verrouillage temporaire requis";
        }

        // Réinitialisation du verrou pour la suite
        session.resetFailedAttempts("caissiere01");
        assert !session.isAccountLocked("caissiere01") : "Le compte ne doit plus être verrouillé après reset";

        System.out.println("OK");
    }

    private static void testBlockedAndDeactivatedAccounts() {
        System.out.print("[TEST 3] Rejet immédiat des comptes BLOQUÉ et DÉSACTIVÉ... ");
        SessionManager session = SessionManager.getInstance();
        session.resetForTests();

        UserService userService = UserService.getInstance();

        // Création d'un utilisateur de test
        User uBlocked = new User(10, "TEST", "BLOCKED", "User", "user_blocked",
                "blocked@test.cd", "+243 00 000 0001", RoleGuard.Role.CAISSIERE, null,
                User.UserStatus.BLOQUE, null, userService.hashPassword("Pass1234!"));
        userService.createUser(uBlocked, "Pass1234!");
        uBlocked.setStatus(User.UserStatus.BLOQUE);

        try {
            session.login("user_blocked", "Pass1234!");
            assert false : "La connexion sur un compte bloqué aurait dû être rejetée";
        } catch (SecurityException e) {
            assert e.getMessage().contains("Ce compte est bloqué") : "Le rejet doit indiquer le blocage";
        }

        // Compte désactivé
        uBlocked.setStatus(User.UserStatus.DESACTIVE);
        try {
            session.login("user_blocked", "Pass1234!");
            assert false : "La connexion sur un compte désactivé aurait dû être rejetée";
        } catch (SecurityException e) {
            assert e.getMessage().contains("Ce compte a été désactivé") : "Le rejet doit indiquer la désactivation";
        }

        System.out.println("OK");
    }

    private static void testRoleTailoredFinancialIsolation() {
        System.out.print("[TEST 4] Cloisonnement étanche et isolation financière par rôle... ");

        // DIRECTEUR : accède à la pédagogie et au personnel, JAMAIS aux finances
        RoleGuard.setCurrentRole(RoleGuard.Role.DIRECTEUR);
        assert !RoleGuard.canViewFinancialData() : "Le Directeur ne peut pas voir les données financières";
        assert !RoleGuard.canCreatePayment() : "Le Directeur ne peut pas créer de paiement";
        assert !RoleGuard.canCloseCash() : "Le Directeur ne peut pas fermer la caisse";
        assert RoleGuard.canAccessStudents() : "Le Directeur accède aux élèves";
        assert RoleGuard.canAccessStaff() : "Le Directeur accède au personnel";

        // ADMIN_TECHNIQUE : administre le système, JAMAIS les finances
        RoleGuard.setCurrentRole(RoleGuard.Role.ADMIN_TECHNIQUE);
        assert !RoleGuard.canViewFinancialData() : "L'Admin technique ne peut pas voir les montants";
        assert !RoleGuard.canCreatePayment() : "L'Admin technique ne peut pas créer de paiement";
        assert !RoleGuard.canCloseCash() : "L'Admin technique ne peut pas clôturer de caisse";
        assert !RoleGuard.can("budget.manage") : "L'Admin technique ne peut pas allouer de budget";
        assert RoleGuard.can("user.manage") : "L'Admin technique peut gérer les utilisateurs";

        // CAISSIERE : finances opérationnelles et caisse
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assert RoleGuard.canViewFinancialData() : "La caissière a accès aux chiffres financiers";
        assert RoleGuard.canCreatePayment() : "La caissière encaisse";
        assert RoleGuard.canCloseCash() : "La caissière clôture sa caisse";
        assert !RoleGuard.can("user.manage") : "La caissière ne peut pas administrer les utilisateurs";

        System.out.println("OK");
    }

    private static void testNotificationServiceOperations() {
        System.out.print("[TEST 5] Service des notifications & protection des alertes critiques... ");
        NotificationService notifService = NotificationService.getInstance();
        notifService.resetForTests();

        int initialCount = notifService.getAllNotifications().size();
        assert initialCount >= 3 : "Au moins 3 notifications initiales configurées";

        // Ajout d'une alerte d'information
        SystemNotification info = notifService.createAlert(
                SystemNotification.Priority.INFO,
                "Maintenance Prévue",
                "Mise à jour système planifiée ce dimanche.",
                Route.SETTINGS
        );
        assert notifService.getAllNotifications().size() == initialCount + 1 : "Le total doit être incrémenté";

        // Marquage comme lue
        assert !info.isRead() : "La notification doit être non lue à la création";
        notifService.markAsRead(info.getId());
        assert info.isRead() : "La notification doit être marquée comme lue";

        // Suppression de la notification informative -> autorisée
        boolean dismissed = notifService.dismissNotification(info.getId());
        assert dismissed : "Une alerte informative peut être supprimée";

        // Tentative de suppression d'une alerte critique persistante -> STRICTEMENT REFUSÉE
        List<SystemNotification> criticals = notifService.getByPriority(SystemNotification.Priority.CRITICAL);
        assert !criticals.isEmpty() : "Une alerte critique doit exister";
        SystemNotification crit = criticals.get(0);

        try {
            notifService.dismissNotification(crit.getId());
            assert false : "La suppression d'une alerte critique sans résolution aurait dû lever une exception";
        } catch (IllegalStateException e) {
            assert e.getMessage().contains("requièrent la résolution") : "Message de rejet explicite requis";
        }

        System.out.println("OK");
    }

    private static void testProfilePasswordUpdateSecurity() {
        System.out.print("[TEST 6] Sécurité du profil & mise à jour contrôlée du mot de passe... ");
        UserService userService = UserService.getInstance();
        User user = userService.findByUsername("caissiere01");
        assert user != null : "L'utilisateur caissiere01 doit exister";

        // 1. Rejet avec ancien mot de passe erroné
        assert !userService.verifyPassword(user, "FauxMotDePasse") : "Le faux mot de passe ne doit pas être validé";

        // 2. Ancien mot de passe valide
        assert userService.verifyPassword(user, "Caisse1234!") : "Le mot de passe initial doit être valide";

        // 3. Mise à jour avec nouveau mot de passe
        userService.resetPassword(user.getId(), "NouveauCaisse5678!");
        assert !userService.verifyPassword(user, "Caisse1234!") : "L'ancien mot de passe ne doit plus être accepté";
        assert userService.verifyPassword(user, "NouveauCaisse5678!") : "Le nouveau mot de passe doit être immédiatement actif";

        // Rétablissement pour idempotence
        userService.resetPassword(user.getId(), "Caisse1234!");
        assert userService.verifyPassword(user, "Caisse1234!") : "Le mot de passe réinitialisé doit être valide";

        System.out.println("OK");
    }

    private static void testAuditLogImmutability() {
        System.out.print("[TEST 7] Immuabilité & intégrité du journal d'audit... ");
        AuditService audit = AuditService.getInstance();
        int initialSize = audit.getAllLogs().size();

        audit.logAction("superviseur", "TEST_EVENEMENT_SÉCURITÉ", "Security", null, "Vérification intégrité");
        List<AuditLog> logs = audit.getAllLogs();
        assert logs.size() == initialSize + 1 : "L'événement doit être ajouté au registre";

        // Copie défensive : modifier la liste externe ne doit pas impacter le service
        logs.clear();
        assert audit.getAllLogs().size() == initialSize + 1 : "La liste retournée par getAllLogs() doit être immuable / copie défensive";

        System.out.println("OK");
    }

    private static void testLogoutFlow() {
        System.out.print("[TEST 8] Flux de déconnexion & réinitialisation de sécurité... ");
        SessionManager session = SessionManager.getInstance();
        session.login("directeur", "Directeur1234!");
        assert session.isLoggedIn() : "L'utilisateur doit être connecté";
        assert RoleGuard.getCurrentRole() == RoleGuard.Role.DIRECTEUR : "Rôle courant DIRECTEUR";

        session.logout();
        assert !session.isLoggedIn() : "La session doit être clôturée après logout";
        assert session.getCurrentUser() == null : "Le currentUser doit être null";
        assert RoleGuard.getCurrentRole() == null : "Le rôle dans RoleGuard doit être effacé";

        // Vérification du log d'audit de déconnexion
        List<AuditLog> logs = AuditService.getInstance().getAllLogs();
        boolean foundLogout = logs.stream().anyMatch(l -> "DÉCONNEXION".equals(l.getAction()) && "directeur".equals(l.getUsername()));
        assert foundLogout : "Un log d'audit de déconnexion doit être tracé";

        System.out.println("OK");
    }
}
