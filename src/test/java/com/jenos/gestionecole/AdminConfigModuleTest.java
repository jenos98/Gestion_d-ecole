package com.jenos.gestionecole;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.*;
import com.jenos.gestionecole.util.RoleGuard;

import java.time.LocalDate;
import java.util.List;

/**
 * AdminConfigModuleTest — Suite de validation automatisée du Bloc 6 :
 * 1. Contrôle d'accès granulaire RoleGuard.can(...) & Isolation stricte de l'Admin Technique
 * 2. Gestion des utilisateurs (CRUD, mot de passe, non-suppression physique & caisse unique)
 * 3. Cycle de vie des années scolaires (activation exclusive & archivage de l'ancienne)
 * 4. Intégrité de la hiérarchie académique (Section → Promotion → Option → Classe)
 * 5. Configuration des modes de paiement (exigence de référence obligatoire)
 * 6. Supervision budgétaire (alertes 80%, 90%, 100% & séparation USD/CDF)
 * 7. Traçabilité et journalisation d'audit (AuditService)
 */
public class AdminConfigModuleTest {

    public static void main(String[] args) {
        System.out.println("=== DÉBUT DES TESTS BLOC 6 (Administration, Configuration & Droits) ===");

        testRoleGuardAndTechnicalAdminIsolation();
        testUserManagementAndNonDestructivePolicy();
        testCashierSingleRegisterRule();
        testAcademicYearLifecycle();
        testAcademicHierarchyIntegrity();
        testPaymentMethodConfig();
        testBudgetThresholdsAndCurrencySeparation();
        testAuditTrail();

        System.out.println("=== TOUS LES TESTS DU BLOC 6 ONT RÉUSSI (100%) ===");
    }

    private static void testRoleGuardAndTechnicalAdminIsolation() {
        System.out.print("[TEST 1] Droits RoleGuard & Isolation financière de l'Admin Technique... ");

        // PROMOTEUR : accès complet
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);
        assert RoleGuard.can("cash.close") : "Le promoteur peut clôturer la caisse";
        assert RoleGuard.can("payment.cancel") : "Le promoteur peut annuler un reçu";
        assert RoleGuard.can("report.financial") : "Le promoteur accède aux rapports financiers";
        assert RoleGuard.can("budget.manage") : "Le promoteur gère le budget";
        assert RoleGuard.canViewFinancialData() : "Le promoteur voit les montants réels";

        // ADMINISTRATEUR TECHNIQUE : configuration permise, MAIS aucun accès aux montants financiers !
        RoleGuard.setCurrentRole(RoleGuard.Role.ADMIN_TECHNIQUE);
        assert RoleGuard.can("user.manage") : "L'admin tech gère les comptes";
        assert RoleGuard.can("academic.manage") : "L'admin tech gère la structure scolaire";
        assert RoleGuard.can("fee.manage") : "L'admin tech configure les frais";
        assert RoleGuard.can("payment_method.manage") : "L'admin tech configure les modes de paiement";

        // VÉRIFICATION CRITIQUE : Cloisonnement financier strict
        assert !RoleGuard.canViewFinancialData() : "L'admin technique NE PEUT PAS voir les montants financiers réels !";
        assert !RoleGuard.can("report.financial") : "L'admin technique NE PEUT PAS générer les rapports financiers !";
        assert !RoleGuard.can("budget.manage") : "L'admin technique NE PEUT PAS allouer ou gérer le budget !";
        assert !RoleGuard.can("payment.create") : "L'admin technique NE PEUT PAS encaisser de paiements !";
        assert !RoleGuard.can("cash.open") : "L'admin technique NE PEUT PAS ouvrir de caisse !";

        // CAISSIÈRE : opérations guichet sans administration
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assert RoleGuard.can("payment.create") : "La caissière encaisse";
        assert RoleGuard.can("cash.close") : "La caissière clôture sa caisse";
        assert !RoleGuard.can("user.manage") : "La caissière n'administre pas les utilisateurs";
        assert !RoleGuard.can("fee.manage") : "La caissière ne configure pas les tarifs";

        System.out.println("OK");
    }

    private static void testUserManagementAndNonDestructivePolicy() {
        System.out.print("[TEST 2] Gestion des utilisateurs & Règle de non-suppression physique... ");
        UserService userService = UserService.getInstance();

        int countBefore = userService.getAllUsers().size();

        // Création d'un nouvel utilisateur
        User newUser = new User(0, "TSHILOMBO", "KABASELE", "Patrick", "patrick_t",
                "patrick@ecole.cd", "+243 89 000 0000", RoleGuard.Role.SECRETARIAT, null,
                User.UserStatus.ACTIF, null, "");
        User created = userService.createUser(newUser, "Secret2026!");
        assert created != null && created.getId() > 0 : "L'utilisateur doit être créé";
        assert userService.getAllUsers().size() == countBefore + 1 : "La liste doit s'incrémenter";

        // RÈGLE ABSOLUE : Pas de DELETE physique lors de la suppression -> Désactivation
        userService.deactivateUser(created.getId(), "Départ de l'employé");
        User deactivated = userService.findById(created.getId());
        assert deactivated != null : "L'utilisateur ne doit pas être supprimé de la base";
        assert deactivated.getStatus() == User.UserStatus.DESACTIVE : "Le statut doit être DESACTIVE";
        assert userService.getAllUsers().size() == countBefore + 1 : "L'effectif physique reste préservé pour l'audit";

        System.out.println("OK");
    }

    private static void testCashierSingleRegisterRule() {
        System.out.print("[TEST 3] Règle métier : Caisse unique par caissière active... ");
        UserService userService = UserService.getInstance();

        // "Caisse Principale 01" est déjà affectée à la caissière active Sarah (ID 2)
        boolean conflictDetected = false;
        try {
            userService.validateCashRegisterAssignment("Caisse Principale 01", 999);
        } catch (IllegalStateException ex) {
            conflictDetected = true;
        }
        assert conflictDetected : "Le système doit rejeter l'attribution d'une même caisse à plusieurs caissières simultanées";

        System.out.println("OK");
    }

    private static void testAcademicYearLifecycle() {
        System.out.print("[TEST 4] Cycle de vie des années scolaires & Archivage automatique... ");
        AcademicYearService yearService = AcademicYearService.getInstance();

        AcademicYear activeBefore = yearService.getActiveYear();
        assert activeBefore != null : "Une année active doit exister initialement";
        assert "2025-2026".equals(activeBefore.getCode()) : "L'année active initiale est 2025-2026";

        // Création d'une nouvelle année
        AcademicYear planned = yearService.createYear("2027-2028", "Année Scolaire 2027-2028",
                LocalDate.of(2027, 9, 1), LocalDate.of(2028, 7, 2));
        assert planned.getStatus() == AcademicYear.YearStatus.PLANIFIEE;

        // Activation de la nouvelle année : l'ancienne doit passer automatiquement à ARCHIVEE
        yearService.activateYear(planned.getId());
        AcademicYear newActive = yearService.getActiveYear();
        assert newActive != null && newActive.getId() == planned.getId() : "La nouvelle année doit être ACTIVE";

        AcademicYear oldActive = yearService.findById(activeBefore.getId());
        assert oldActive.getStatus() == AcademicYear.YearStatus.ARCHIVEE : "L'ancienne année active doit être ARCHIVEE";

        // Rétablir 2025-2026 active pour la cohérence des autres blocs
        yearService.activateYear(activeBefore.getId());

        System.out.println("OK");
    }

    private static void testAcademicHierarchyIntegrity() {
        System.out.print("[TEST 5] Hiérarchie Section → Promotion → Option → Classe... ");
        AcademicStructureService structure = AcademicStructureService.getInstance();

        List<Section> sections = structure.findAllSections();
        assert sections.size() >= 2 : "Au moins deux sections configurées";

        Section secSecondaire = sections.stream().filter(s -> "Secondaire".equalsIgnoreCase(s.getName())).findFirst().orElse(null);
        assert secSecondaire != null;

        List<Promotion> promos = structure.findPromotionsBySection(secSecondaire);
        assert !promos.isEmpty() : "Le Secondaire doit comporter des promotions";

        Promotion p6 = promos.stream().filter(p -> p.getName().contains("6")).findFirst().orElse(null);
        assert p6 != null;

        // Promotion 1 ── N Options
        List<AcademicOption> options = structure.findOptionsByPromotion(p6);
        assert !options.isEmpty() : "La 6ème doit avoir des options (Scientifique, etc.)";

        // Option 1 ── N Classes
        AcademicOption optSc = options.stream().filter(o -> "Scientifique".equalsIgnoreCase(o.getName())).findFirst().orElse(null);
        assert optSc != null;

        List<Classe> classes = structure.findClassesByOption(optSc);
        assert !classes.isEmpty() : "L'option Scientifique doit contenir des classes";

        for (Classe c : classes) {
            assert c.getOption().getId() == optSc.getId() : "La classe doit être liée à son option";
            assert c.getPromotion().getId() == p6.getId() : "La classe doit appartenir à sa promotion";
        }

        System.out.println("OK");
    }

    private static void testPaymentMethodConfig() {
        System.out.print("[TEST 6] Configuration des modes de paiement & Exigence de référence... ");
        PaymentMethodService methodService = PaymentMethodService.getInstance();

        PaymentMethodConfig mobile = methodService.findByCode("MOBILE_MONEY");
        assert mobile != null : "Le mode MOBILE_MONEY doit exister";
        assert mobile.isRequiresReference() : "Mobile Money exige impérativement une référence de transaction";

        PaymentMethodConfig especes = methodService.findByCode("ESPECES");
        assert especes != null : "Le mode ESPECES doit exister";
        assert !especes.isRequiresReference() : "Espèces ne nécessite pas de référence obligatoire";

        // Désactivation non destructive
        methodService.toggleActive(mobile.getId());
        assert !mobile.isActive() : "Le statut doit basculer à inactif";
        methodService.toggleActive(mobile.getId()); // Réactiver

        System.out.println("OK");
    }

    private static void testBudgetThresholdsAndCurrencySeparation() {
        System.out.print("[TEST 7] Seuils budgétaires (80%, 90%, 100%) & Séparation USD / CDF... ");
        BudgetService budgetService = BudgetService.getInstance();

        // 1. Budget avec alerte 90%
        Budget b90 = new Budget(10, "Test 90", "BDG-TEST", 1000.0, 920.0, "USD", "2025-2026");
        assert b90.isWarning90() : "Doit déclencher l'alerte de seuil 90%";
        assert !b90.isExceeded100() : "Ne doit pas être dépassé à 100%";

        // 2. Budget avec alerte 80%
        Budget b80 = new Budget(11, "Test 80", "BDG-TEST2", 1000.0, 830.0, "USD", "2025-2026");
        assert b80.isWarning80() : "Doit déclencher l'alerte de seuil 80%";

        // 3. Budget dépassé (100%+)
        Budget b100 = new Budget(12, "Test 100", "BDG-TEST3", 1000.0, 1050.0, "USD", "2025-2026");
        assert b100.isExceeded100() : "Doit déclencher le statut dépassé (>=100%)";

        // 4. Séparation stricte USD / CDF
        double totalAllocUSD = budgetService.calculateTotalAllocatedUSD();
        double totalAllocCDF = budgetService.calculateTotalAllocatedCDF();
        assert totalAllocUSD > 0 : "Total alloué USD distinct";
        assert totalAllocCDF > 0 : "Total alloué CDF distinct";
        assert totalAllocUSD != totalAllocCDF : "Les devises USD et CDF sont strictement indépendantes";

        System.out.println("OK");
    }

    private static void testAuditTrail() {
        System.out.print("[TEST 8] Journalisation d'audit des modifications sensibles... ");
        AuditService auditService = AuditService.getInstance();

        int logsBefore = auditService.getLogCount();
        auditService.logAction("Promoteur", "MODIFICATION_FRAIS", "Fee", "150000 CDF", "175000 CDF");
        assert auditService.getLogCount() == logsBefore + 1 : "Chaque modification sensible doit être journalisée";

        AuditLog lastLog = auditService.getAllLogs().get(auditService.getAllLogs().size() - 1);
        assert "Promoteur".equals(lastLog.getUsername());
        assert "MODIFICATION_FRAIS".equals(lastLog.getAction());
        assert "150000 CDF".equals(lastLog.getOldValue());
        assert "175000 CDF".equals(lastLog.getNewValue());

        System.out.println("OK");
    }
}
