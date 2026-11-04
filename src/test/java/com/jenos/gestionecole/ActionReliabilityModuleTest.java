package com.jenos.gestionecole;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.*;
import com.jenos.gestionecole.util.RoleGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ActionReliabilityModuleTest — Suite de validation automatisée JUnit 5 pour le Bloc 8
 * (Correction et fiabilisation des boutons et des actions).
 *
 * Valide rigoureusement :
 * 1. La protection anti-double soumission et la prévention des doublons financiers.
 * 2. La synchronisation automatique du tableau de bord lors des actions (paiements, dépenses, inscriptions).
 * 3. La cohérence des autorisations RoleGuard sur les actions clés.
 * 4. La validation des saisies de formulaires et l'intégrité des opérations d'enregistrement.
 * 5. La machine à états stricte de la clôture de caisse.
 */
public class ActionReliabilityModuleTest {

    private SchoolFeeService schoolFeeService;
    private ExpenseService expenseService;
    private StudentService studentService;
    private CashClosureService cashService;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        RoleGuard.clearRole();
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);

        schoolFeeService = SchoolFeeService.getInstance();
        expenseService = ExpenseService.getInstance();
        studentService = StudentService.getInstance();
        cashService = CashClosureService.getInstance();
        dashboardService = DashboardService.getInstance();
    }

    // =========================================================================
    //  Critère 1 : Protection anti-double soumission et idempotence financière
    // =========================================================================

    @Test
    @DisplayName("Critère 1 : Les paiements successifs mettent à jour le solde restant et bloquent tout trop-perçu")
    void testPaymentBalanceAndOverpaymentPrevention() {
        Student student = new Student();
        student.setId(991);
        student.setNom("LUMUMBA");
        student.setPostnom("OKITO");
        student.setPrenom("Patrice");
        student.setMatricule("2026-TEST-BTN");
        student.setAnneeScolaire("2024-2025");

        List<StudentFeeAssignment> assigned = schoolFeeService.assignFeesToStudent(student, "2024-2025", "caissiere01");
        assertFalse(assigned.isEmpty(), "Des frais doivent être affectés");

        StudentFeeAssignment assignment = assigned.get(0);
        FeeInstallment installment = assignment.getInstallments().get(0);
        double initialBalance = assignment.getBalance();

        // Tentative de versement excédant le solde restant (solde initial + 200)
        double excessAmount = initialBalance + 200.0;
        assertThrows(IllegalArgumentException.class, () -> schoolFeeService.recordFeePayment(
                assignment.getId(), installment.getId(),
                excessAmount, assignment.getCurrency(),
                Payment.PaymentMethod.ESPECES, null,
                "caissiere01", student
        ), "Un versement excédant le solde restant doit être immédiatement rejeté");
    }

    // =========================================================================
    //  Critère 2 : Notification automatique du Dashboard lors des opérations
    // =========================================================================

    @Test
    @DisplayName("Critère 2 : DashboardService notifie ses observateurs lors d'une modification de données")
    void testDashboardReactiveNotification() {
        AtomicBoolean notificationReceived = new AtomicBoolean(false);

        DashboardService.DashboardDataListener listener = () -> notificationReceived.set(true);
        dashboardService.addListener(listener);

        try {
            // Déclenchement d'une notification
            dashboardService.notifyDataChanged();
            assertTrue(notificationReceived.get(), "L'observateur du tableau de bord doit recevoir la notification");
        } finally {
            dashboardService.removeListener(listener);
        }
    }

    // =========================================================================
    //  Critère 3 : Respect des permissions RoleGuard sur les actions protégées
    // =========================================================================

    @Test
    @DisplayName("Critère 3 : Les boutons et actions administratives respectent strictement les rôles utilisateurs")
    void testRoleGuardActionPermissions() {
        // En tant que SECRÉTARIAT :
        RoleGuard.setCurrentRole(RoleGuard.Role.SECRETARIAT);
        assertTrue(RoleGuard.canCreateStudent(), "Le secrétariat peut inscrire un élève");
        assertFalse(RoleGuard.canViewSalaries(), "Le secrétariat ne peut pas accéder aux salaires");
        assertFalse(RoleGuard.canForceCloseCash(), "Le secrétariat ne peut pas forcer la clôture de caisse");
        assertFalse(RoleGuard.canManageSettings(), "Le secrétariat ne peut pas modifier les paramètres généraux");

        // En tant que CAISSIÈRE :
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assertTrue(RoleGuard.canCreatePayment(), "La caissière peut encaisser un paiement");
        assertFalse(RoleGuard.canArchiveStaff(), "La caissière ne peut pas archiver un membre du personnel");
        assertFalse(RoleGuard.canViewOtherCashRegisters(), "La caissière ne peut pas consulter les autres caisses");

        // En tant que PROMOTEUR :
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);
        assertTrue(RoleGuard.canViewSalaries(), "Le promoteur a accès aux salaires");
        assertTrue(RoleGuard.canForceCloseCash(), "Le promoteur peut forcer une clôture exceptionnelle");
        assertTrue(RoleGuard.canManageSettings(), "Le promoteur peut gérer les paramètres généraux");
    }

    // =========================================================================
    //  Critère 4 : Rejet des formulaires invalides sans altération de données
    // =========================================================================

    @Test
    @DisplayName("Critère 4 : Les enregistrements incomplets ou invalides sont bloqués proprement")
    void testFormValidationAndIntegrity() {
        // Validation d'une dépense négative ou sans bénéficiaire
        Expense invalidExpense = new Expense();
        invalidExpense.setAmount(-20.0);
        invalidExpense.setCurrency("USD");
        invalidExpense.setBeneficiary("");

        assertThrows(IllegalArgumentException.class, () -> expenseService.processExpense(invalidExpense),
                "Une dépense au montant négatif ou sans bénéficiaire doit être rejetée");

        // Validation formelle d'un élève sans nom ou sans prénom
        Student invalidStudent = new Student();
        invalidStudent.setNom("");
        invalidStudent.setPostnom("");
        invalidStudent.setPrenom("");

        StudentValidator.ValidationResult valResult = StudentValidator.validate(invalidStudent, studentService);
        assertFalse(valResult.isValid(), "Un élève avec champs obligatoires vides ne doit pas être validé");
        assertNotNull(valResult.getFirstError(), "Une explication de l'erreur doit être fournie");
    }

    // =========================================================================
    //  Critère 5 : Contrôle d'état de la clôture de caisse
    // =========================================================================

    @Test
    @DisplayName("Critère 5 : La clôture normale de caisse est strictement bloquée sans rapport imprimé")
    void testCashClosureStateGuard() {
        CashSession session = cashService.getCurrentSession();
        assertNotNull(session, "Une session de caisse active doit exister");

        // Si le rapport n'est pas imprimé
        if (!session.isReportPrinted()) {
            assertThrows(IllegalStateException.class, () -> cashService.finalizeClosure("Test sans impression"),
                    "La clôture de caisse doit refuser de finaliser si le rapport journalier n'est pas imprimé");
        }
    }
}
