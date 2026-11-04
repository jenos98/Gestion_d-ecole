package com.jenos.gestionecole;

import cd.ecole.gestionscolaire.config.DatabaseConfig;
import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.*;
import com.jenos.gestionecole.util.RoleGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalStabilizationTest — Suite de validation globale et transverse pour le Bloc 9
 * (Vérification générale, tests et stabilisation avant livraison).
 *
 * Valide l'intégration de bout en bout des 9 blocs fonctionnels :
 * 1. Cycle Scolaire & Financier : Inscription → Frais → Paiements séparés USD/CDF → Soldes.
 * 2. Cycle RH & Rémunération : Personnel → Avance (plafond 50%) → Paie nette → Bulletin.
 * 3. Cycle Trésorerie : Caisse → Dépenses → Rapport obligatoire → Clôture.
 * 4. Audit & Sécurité : Traçabilité immuable, masquage des secrets, cloisonnement des rôles.
 * 5. Configuration PostgreSQL : Validation syntaxique, gestion des paramètres et résilience.
 */
public class GlobalStabilizationTest {

    private SchoolFeeService feeService;
    private PaymentService paymentService;
    private ExpenseService expenseService;
    private StaffService staffService;
    private StaffPayrollService payrollService;
    private CashClosureService cashService;
    private AuditService auditService;
    private StudentService studentService;

    @BeforeEach
    void setUp() {
        RoleGuard.clearRole();
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);

        feeService = SchoolFeeService.getInstance();
        paymentService = PaymentService.getInstance();
        expenseService = ExpenseService.getInstance();
        staffService = StaffService.getInstance();
        payrollService = StaffPayrollService.getInstance();
        cashService = CashClosureService.getInstance();
        auditService = AuditService.getInstance();
        studentService = StudentService.getInstance();
    }

    // =========================================================================
    //  1. Intégration Scolaire & Financière de bout en bout
    // =========================================================================

    @Test
    @DisplayName("Scénario 1 : Admission élève, affectation des frais et encaissements partiels USD/CDF")
    void testEndToEndStudentEnrollmentAndFeePayments() {
        Student student = new Student();
        student.setId(1099);
        student.setNom("KASAVUBU");
        student.setPostnom("JOSEPH");
        student.setPrenom("Arthur");
        student.setMatricule("2026-STAB-001");
        student.setSexe("M");
        student.setAnneeScolaire("2024-2025");
        studentService.save(student);

        // 1. Affectation automatique des frais scolaires (snapshot immuable)
        List<StudentFeeAssignment> assigned = feeService.assignFeesToStudent(student, "2024-2025", "caissiere01");
        assertFalse(assigned.isEmpty(), "Des frais doivent être affectés lors de l'admission");

        StudentFeeAssignment feeUSD = assigned.stream()
                .filter(a -> "USD".equalsIgnoreCase(a.getCurrency()))
                .findFirst().orElseThrow();

        StudentFeeAssignment feeCDF = assigned.stream()
                .filter(a -> "CDF".equalsIgnoreCase(a.getCurrency()))
                .findFirst().orElseThrow();

        double initialUsdBal = feeUSD.getBalance();
        double initialCdfBal = feeCDF.getBalance();

        assertTrue(initialUsdBal > 0, "Le solde initial USD doit être positif");
        assertTrue(initialCdfBal > 0, "Le solde initial CDF doit être positif");

        // 2. Encaissement partiel en USD
        double paymentUsd = Math.min(25.0, initialUsdBal);
        FeeInstallment instUsd = feeUSD.getInstallments().get(0);
        Receipt rUsd = feeService.recordFeePayment(
                feeUSD.getId(), instUsd.getId(), paymentUsd, "USD",
                Payment.PaymentMethod.ESPECES, null, "caissiere01", student
        );
        assertNotNull(rUsd, "Le reçu USD doit être émis");
        assertEquals("USD", rUsd.getCurrency());
        assertEquals(initialUsdBal - paymentUsd, feeUSD.getBalance(), 0.01, "Le solde restant USD doit diminuer exactement");

        // 3. Encaissement partiel en CDF (compartiment strictement séparé)
        double paymentCdf = Math.min(5000.0, initialCdfBal);
        FeeInstallment instCdf = feeCDF.getInstallments().get(0);
        Receipt rCdf = feeService.recordFeePayment(
                feeCDF.getId(), instCdf.getId(), paymentCdf, "CDF",
                Payment.PaymentMethod.ESPECES, null, "caissiere01", student
        );
        assertNotNull(rCdf, "Le reçu CDF doit être émis");
        assertEquals("CDF", rCdf.getCurrency());
        assertEquals(initialCdfBal - paymentCdf, feeCDF.getBalance(), 0.01, "Le solde restant CDF doit diminuer exactement");

        // 4. Traçabilité d'audit générée
        List<AuditLog> logs = auditService.searchLogs(null, null, AuditLog.AuditModule.FINANCES, null, null, null);
        assertFalse(logs.isEmpty(), "Les paiements doivent être consignés dans le journal d'audit");
    }

    // =========================================================================
    //  2. Intégration RH, Avances plafonnées & Paie nette
    // =========================================================================

    @Test
    @DisplayName("Scénario 2 : Gestion du personnel, avance plafonnée à 50% et liquidation de paie")
    void testEndToEndStaffAdvanceAndPayroll() {
        Staff staff = new Staff();
        staff.setNom("ILUNGA");
        staff.setPostnom("TSHIMANGA");
        staff.setPrenom("David");
        staff.setGenre("M");
        staff.setCategorie("Enseignant");
        staff.setFonction("Professeur de Mathématiques");
        staff.setSalaireBase(600.0);
        staff.setDeviseSalaire("USD");
        staff.setStatut(Staff.StaffStatus.ACTIF);
        payrollService.saveStaff(staff, "directeur");

        int staffId = staff.getId();

        // 1. Demande d'avance valide (ex: 200 USD <= 300 USD max)
        SalaryAdvance adv = payrollService.grantAdvance(staffId, 200.0, "USD", "Frais médicaux", "directeur");
        assertNotNull(adv);
        assertEquals(SalaryAdvance.AdvanceStatus.EN_COURS, adv.getStatus());

        // 2. Demande d'avance excessive (> 50% du salaire : 350 USD > 300 USD)
        assertThrows(IllegalArgumentException.class, () ->
                payrollService.grantAdvance(staffId, 350.0, "USD", "Achat véhicule", "directeur"),
                "Une avance dépassant 50% du salaire contractuel doit être strictement refusée");

        // 3. Préparation et liquidation de la paie du mois
        SalaryRecord rec = payrollService.prepareSalary(
                staffId, "11/2026", 11, 2026,
                SalaryRecord.Frequency.MENSUELLE, 50.0, 0.0, 0.0, 200.0
        );
        assertNotNull(rec);
        // Base (600) + Prime (50) - Avance (200) = Net (450 USD)
        assertEquals(450.0, rec.getNetSalary(), 0.01, "Le salaire net doit déduire automatiquement l'avance accordée");

        payrollService.validateSalary(rec.getId(), "Directeur");
        Payslip payslip = payrollService.generatePayslip(rec.getId());
        assertNotNull(payslip, "Le bulletin de paie officiel doit être généré");
        assertEquals(450.0, payslip.getNetPayable(), 0.01);
    }

    // =========================================================================
    //  3. Intégration Caisse, Décaissements & Clôture obligatoire
    // =========================================================================

    @Test
    @DisplayName("Scénario 3 : Engagement d'une dépense, contrôle du solde et machine à états de clôture")
    void testEndToEndCashAndExpenseLifecycle() {
        CashSession session = cashService.getCurrentSession();
        assertNotNull(session, "Une session active de caisse doit exister");

        // 1. Dépense avec contrôle de solde
        Expense exp = new Expense();
        exp.setCategory("FOURNITURES");
        exp.setLabel("Achat rames de papier A4");
        exp.setBeneficiary("Papeterie Centrale");
        exp.setAmount(30.0);
        exp.setCurrency("USD");
        exp.setPaymentMethod(Expense.PaymentMethod.ESPECES);
        exp.setExpenseDate(LocalDate.now());

        Expense processed = expenseService.processExpense(exp);
        assertNotNull(processed.getVoucherNumber(), "Un bon de sortie officiel doit être émis");

        // 2. Machine à états de clôture
        if (!session.isReportPrinted()) {
            assertThrows(IllegalStateException.class, () -> cashService.finalizeClosure("Clôture sans impression"),
                    "La clôture sans impression du rapport journalier doit être formellement rejetée");
        }

        // 3. Passage à l'état imprimé puis réconciliation
        cashService.markDailyReportPrinted();
        assertTrue(session.isReportPrinted(), "Le rapport doit être marqué imprimé");

        cashService.recordCounting(session.getCurrentBalanceUSD(), session.getCurrentBalanceCDF());
        // Clôture sécurisée
        assertDoesNotThrow(() -> cashService.finalizeClosure("Clôture de test réussie"));
    }

    // =========================================================================
    //  4. Sécurité, Rôles & Immutabilité de l'Audit
    // =========================================================================

    @Test
    @DisplayName("Scénario 4 : Vérification du cloisonnement des rôles RoleGuard et de l'immutabilité de l'audit")
    void testSecurityRoleGuardAndAuditImmutability() {
        // En tant que caissière :
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assertFalse(RoleGuard.canViewAuditLog(), "La caissière ne doit pas pouvoir consulter le journal d'audit");
        assertFalse(RoleGuard.canViewSalaries(), "La caissière ne doit pas pouvoir consulter les salaires");

        // En tant que promoteur :
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);
        assertTrue(RoleGuard.canViewAuditLog(), "Le promoteur doit pouvoir consulter le journal d'audit");
        assertTrue(RoleGuard.canViewSalaries(), "Le promoteur doit pouvoir consulter les salaires");

        // L'audit est immuable en lecture seule
        List<AuditLog> auditList = auditService.getAllLogs();
        assertThrows(UnsupportedOperationException.class, () -> auditList.clear(),
                "Le journal d'audit ne doit jamais être modifiable ou supprimable physiquement");
    }

    // =========================================================================
    //  5. Configuration et Résilience de la Base de Données PostgreSQL
    // =========================================================================

    @Test
    @DisplayName("Scénario 5 : Validation de la configuration PostgreSQL DatabaseConfig")
    void testPostgreSQLConfigurationIntegrity() {
        assertDoesNotThrow(DatabaseConfig::validateConfiguration,
                "La configuration PostgreSQL par défaut ou d'environnement doit être valide");

        String jdbcUrl = DatabaseConfig.getJdbcUrl();
        assertNotNull(jdbcUrl, "L'URL JDBC ne doit pas être nulle");
        assertTrue(jdbcUrl.startsWith("jdbc:postgresql://"), "L'URL doit cibler PostgreSQL");

        String user = DatabaseConfig.getUsername();
        assertNotNull(user, "L'utilisateur PostgreSQL ne doit pas être null");
        assertFalse(user.isBlank(), "L'utilisateur ne doit pas être vide");
    }
}
