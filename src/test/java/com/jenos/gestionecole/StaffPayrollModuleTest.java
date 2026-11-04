package com.jenos.gestionecole;

import com.jenos.gestionecole.model.SalaryAdvance;
import com.jenos.gestionecole.model.SalaryPayment;
import com.jenos.gestionecole.model.SalaryRecord;
import com.jenos.gestionecole.model.Staff;
import com.jenos.gestionecole.service.StaffPayrollService;
import com.jenos.gestionecole.util.RoleGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StaffPayrollModuleTest — Suite de validation automatisée JUnit 5 pour le Bloc 2 (Gestion du personnel).
 *
 * Valide rigoureusement :
 * 1. Gestion des dossiers employés et archivage non destructif (préservation de l'historique).
 * 2. Prise en charge des fréquences de rémunération (Mensuelle, Quinzainière, etc.).
 * 3. Exactitude de la formule officielle de calcul du salaire net avec arrondi à l'unité.
 * 4. Gestion et suivi individuel des avances sur salaire (déduction et protection anti-double décompte).
 * 5. Versements complets et partiels avec recalcul dynamique du solde restant.
 * 6. Protection contre les doubles paiements et les surpaiements.
 * 7. Séparation stricte des monnaies USD / CDF sans conversion automatique.
 * 8. Immuabilité des rémunérations validées (insensibilité aux modifications ultérieures du salaire de base).
 * 9. Respect des rôles et autorisations.
 * 10. Génération et export PDF du bulletin de paie officiel via Apache PDFBox.
 */
public class StaffPayrollModuleTest {

    private StaffPayrollService payrollService;

    @BeforeEach
    void setUp() {
        payrollService = StaffPayrollService.getInstance();
    }

    @Test
    @DisplayName("Critère 1 : Dossier employé, matricule unique et archivage non destructif avec conservation de l'historique")
    void testStaffManagementAndNonDestructiveArchiving() {
        Staff staff = new Staff();
        staff.setMatricule("STF-TEST-2026");
        staff.setNom("LUKUSA");
        staff.setPostnom("TSHIMANGA");
        staff.setPrenom("Junior");
        staff.setCategorie("Enseignant");
        staff.setFonction("Professeur de Physique");
        staff.setContractType("CDI");
        staff.setServiceAffectation("Secondaire Scientifique");
        staff.setSalaireBase(500.0);
        staff.setDeviseSalaire("USD");
        staff.setDateEmbauche(LocalDate.of(2022, 9, 1));

        payrollService.saveStaff(staff, "directeur");
        assertTrue(staff.getId() > 0, "L'employé doit recevoir un identifiant généré");

        // Préparation d'une rémunération
        SalaryRecord rec = payrollService.prepareSalary(
                staff.getId(), "09/2026", 9, 2026,
                SalaryRecord.Frequency.MENSUELLE, 50.0, 20.0, 10.0, 0.0
        );
        payrollService.validateSalary(rec.getId(), "directeur");

        // Archivage
        boolean archived = payrollService.archiveStaff(staff.getId(), "directeur");
        assertTrue(archived, "L'archivage doit retourner true");

        Staff reloaded = payrollService.findStaffById(staff.getId());
        assertNotNull(reloaded, "L'employé archivé doit toujours être consultable (aucun DELETE physique)");
        assertEquals(Staff.StaffStatus.ARCHIVE, reloaded.getStatut());

        // Vérification de la préservation de l'historique salarial
        List<SalaryRecord> history = payrollService.getSalaryRepository().findRecordsByStaff(staff.getId());
        assertFalse(history.isEmpty(), "L'historique des rémunérations doit être intégralement conservé après archivage");
        assertEquals(rec.getId(), history.get(0).getId());
    }

    @Test
    @DisplayName("Critère 2 & 3 : Prise en charge des fréquences et exactitude de la formule officielle du salaire net")
    void testSalaryCalculationFormulaAndFrequencies() {
        Staff staff = payrollService.findStaffById(1); // Alain Mukendi, base 450 USD
        assertNotNull(staff);

        // Formule : Salaire net = Salaire de base (450) + Primes fixes (80) + Primes variables (45) - Retenues (25) - Avances (50)
        // Net attendu = 450 + 80 + 45 - 25 - 50 = 500 USD
        SalaryRecord record = payrollService.prepareSalary(
                staff.getId(), "10/2026", 10, 2026,
                SalaryRecord.Frequency.QUINZAINIERE,
                80.0, 45.0, 25.0, 50.0
        );

        assertEquals(SalaryRecord.Frequency.QUINZAINIERE, record.getFrequency());
        assertEquals(450.0, record.getBaseSalary());
        assertEquals(80.0, record.getFixedAllowances());
        assertEquals(45.0, record.getVariableAllowances());
        assertEquals(25.0, record.getDeductions());
        assertEquals(50.0, record.getAdvancesDeducted());

        assertEquals(500.0, record.getNetSalary(), "Le salaire net doit être exactement 500 USD selon la formule officielle");
        assertEquals(500.0, record.getRemainingBalance(), "Le solde initial restant dû doit être égal au salaire net");
        assertEquals(SalaryRecord.PaymentStatus.NON_PAYE, record.getPaymentStatus());
    }

    @Test
    @DisplayName("Critère 4 : Suivi individuel des avances, déduction contrôlée et protection anti-double décompte")
    void testSalaryAdvanceTrackingAndDeductionProtection() {
        Staff staff = payrollService.findStaffById(2); // Marie Kabange, 600 USD
        assertNotNull(staff);

        // 1. Octroi d'une avance de 200 USD
        SalaryAdvance advance = payrollService.grantAdvance(staff.getId(), 200.0, "USD", "Avance équipement", "directeur");
        assertEquals(200.0, advance.getInitialAmount());
        assertEquals(200.0, advance.getRemainingBalance());
        assertEquals(SalaryAdvance.AdvanceStatus.EN_COURS, advance.getStatus());

        // 2. Préparation et validation d'une paie déduisant 150 USD
        SalaryRecord rec1 = payrollService.prepareSalary(staff.getId(), "11/2026", 11, 2026,
                SalaryRecord.Frequency.MENSUELLE, 0, 0, 0, 150.0);
        payrollService.validateSalary(rec1.getId(), "directeur");

        SalaryAdvance advUpdated = payrollService.getSalaryRepository().findAdvanceById(advance.getId()).orElseThrow();
        assertEquals(50.0, advUpdated.getRemainingBalance(), "Le solde restant de l'avance doit être de 50 USD");
        assertEquals(SalaryAdvance.AdvanceStatus.EN_COURS, advUpdated.getStatus());

        // 3. Deuxième paie : tentative de déduire 100 USD (alors qu'il ne reste que 50 USD)
        SalaryRecord rec2 = payrollService.prepareSalary(staff.getId(), "12/2026", 12, 2026,
                SalaryRecord.Frequency.MENSUELLE, 0, 0, 0, 100.0);

        // Le système doit plafonner la déduction au solde réellement restant (50 USD)
        assertEquals(50.0, rec2.getAdvancesDeducted(), "La déduction doit être automatiquement limitée au solde restant");
        payrollService.validateSalary(rec2.getId(), "directeur");

        SalaryAdvance advFinished = payrollService.getSalaryRepository().findAdvanceById(advance.getId()).orElseThrow();
        assertEquals(0.0, advFinished.getRemainingBalance(), "Le solde restant doit être 0");
        assertEquals(SalaryAdvance.AdvanceStatus.REMBOURSEE, advFinished.getStatus(), "L'avance doit être marquée remboursée");

        // 4. Troisième paie : aucune avance ne doit plus être disponible à déduire
        assertEquals(0.0, payrollService.getTotalActiveAdvances(staff.getId(), "USD"), "Aucune avance active ne doit subsister");
    }

    @Test
    @DisplayName("Critère 5 & 6 : Versements échelonnés, calcul du solde restant et blocage du double paiement")
    void testPartialPaymentsRemainingBalanceAndDoublePaymentPrevention() {
        Staff staff = payrollService.findStaffById(3); // David Kashama, 400 USD
        assertNotNull(staff);

        SalaryRecord rec = payrollService.prepareSalary(staff.getId(), "01/2027", 1, 2027,
                SalaryRecord.Frequency.MENSUELLE, 0, 0, 0, 0);
        payrollService.validateSalary(rec.getId(), "directeur");

        assertEquals(400.0, rec.getNetSalary());
        assertEquals(400.0, rec.getRemainingBalance());
        assertEquals(SalaryRecord.PaymentStatus.NON_PAYE, rec.getPaymentStatus());

        // 1er versement partiel : 150 USD
        SalaryPayment p1 = payrollService.recordPayment(rec.getId(), 150.0, "USD", "ESPECES", "REF-001", "caissiere01", "Acompte 1");
        assertNotNull(p1);
        assertEquals(150.0, rec.getTotalPaid());
        assertEquals(250.0, rec.getRemainingBalance(), "Le solde restant doit être de 250 USD");
        assertEquals(SalaryRecord.PaymentStatus.PARTIELLEMENT_PAYE, rec.getPaymentStatus());

        // 2ème versement partiel : 100 USD
        SalaryPayment p2 = payrollService.recordPayment(rec.getId(), 100.0, "USD", "MOBILE_MONEY", "REF-002", "caissiere01", "Acompte 2");
        assertNotNull(p2);
        assertEquals(250.0, rec.getTotalPaid());
        assertEquals(150.0, rec.getRemainingBalance(), "Le solde restant doit être de 150 USD");
        assertEquals(SalaryRecord.PaymentStatus.PARTIELLEMENT_PAYE, rec.getPaymentStatus());

        // Tentative de surpaiement (verser 200 USD alors que le solde est 150 USD)
        assertThrows(IllegalArgumentException.class, () ->
            payrollService.recordPayment(rec.getId(), 200.0, "USD", "ESPECES", "REF-ERR", "caissiere01", "Surpaiement")
        , "Le système doit interdire un paiement supérieur au solde restant");

        // 3ème versement libératoire : solde exact 150 USD
        SalaryPayment p3 = payrollService.recordPayment(rec.getId(), 150.0, "USD", "ESPECES", "REF-003", "caissiere01", "Solde final");
        assertNotNull(p3);
        assertEquals(400.0, rec.getTotalPaid());
        assertEquals(0.0, rec.getRemainingBalance(), "Le solde doit être exactement 0");
        assertEquals(SalaryRecord.PaymentStatus.PAYE_INTEGRALEMENT, rec.getPaymentStatus());
        assertEquals(SalaryRecord.ValidationStatus.CLOTUREE, rec.getValidationStatus());

        // Tentative de double paiement intégral sur un salaire déjà soldé
        assertThrows(IllegalArgumentException.class, () ->
            payrollService.recordPayment(rec.getId(), 50.0, "USD", "ESPECES", "REF-FAIL", "caissiere01", "Double paiement")
        , "Un salaire déjà soldé ne peut pas recevoir de paiements additionnels");
    }

    @Test
    @DisplayName("Critère 7 : Séparation stricte USD / CDF (interdiction absolue de mélange ou conversion implicite)")
    void testStrictCurrencySeparation() {
        Staff staffUSD = payrollService.findStaffById(1); // Alain Mukendi (USD)
        SalaryRecord recUSD = payrollService.prepareSalary(staffUSD.getId(), "02/2027", 2, 2027,
                SalaryRecord.Frequency.MENSUELLE, 0, 0, 0, 0);
        payrollService.validateSalary(recUSD.getId(), "directeur");

        // Tentative de payer en CDF un salaire libellé en USD
        assertThrows(IllegalArgumentException.class, () ->
            payrollService.recordPayment(recUSD.getId(), 1000000.0, "CDF", "ESPECES", "REF-CDF", "caissiere01", "Paiement en mauvaise devise")
        , "Le système doit rejeter formellement toute tentative de paiement dans une devise distincte");

        // Tentative d'octroyer une avance en CDF à un agent rémunéré en USD
        assertThrows(IllegalArgumentException.class, () ->
            payrollService.grantAdvance(staffUSD.getId(), 50000.0, "CDF", "Avance CDF", "directeur")
        , "L'avance doit obligatoirement respecter la monnaie du contrat");
    }

    @Test
    @DisplayName("Critère 8 : Immuabilité absolue des rémunérations validées face aux modifications ultérieures du salaire de base")
    void testHistoricalImmutabilityOfValidatedSalary() {
        Staff staff = payrollService.findStaffById(4); // Solange Nyemba, salaire initial 300 USD
        assertNotNull(staff);
        assertEquals(300.0, staff.getSalaireBase());

        // Rémunération validée pour Mars 2026 à 300 USD
        SalaryRecord record = payrollService.prepareSalary(staff.getId(), "03/2026", 3, 2026,
                SalaryRecord.Frequency.MENSUELLE, 20.0, 0.0, 0.0, 0.0);
        payrollService.validateSalary(record.getId(), "directeur");
        assertEquals(320.0, record.getNetSalary());

        // Simulation : En Septembre 2026, Solange bénéficie d'une augmentation de salaire de base (de 300 à 450 USD)
        staff.setSalaireBase(450.0);
        payrollService.saveStaff(staff, "directeur");

        // VÉRIFICATION D'IMMUABILITÉ HISTORIQUE :
        // L'ancienne rémunération validée de Mars 2026 DOIT CONSERVER 300 USD de base et 320 USD de net !
        SalaryRecord historical = payrollService.getSalaryRepository().findRecordById(record.getId()).orElseThrow();
        assertEquals(300.0, historical.getBaseSalary(), "Le salaire de base historique de la paie validée ne doit PAS changer");
        assertEquals(320.0, historical.getNetSalary(), "Le salaire net validé doit demeurer rigoureusement immuable");

        // La nouvelle rémunération d'Octobre 2026 bénéficiera, elle, du nouveau salaire
        SalaryRecord newRecord = payrollService.prepareSalary(staff.getId(), "10/2026", 10, 2026,
                SalaryRecord.Frequency.MENSUELLE, 0.0, 0.0, 0.0, 0.0);
        assertEquals(450.0, newRecord.getBaseSalary(), "La nouvelle paie doit intégrer le salaire actualisé");
    }

    @Test
    @DisplayName("Critère 9 : Permissions RoleGuard (Directeur et Promoteur habilités)")
    void testRoleGuardPermissions() {
        RoleGuard.setCurrentRole(RoleGuard.Role.DIRECTEUR);
        assertTrue(RoleGuard.canManagePayroll(), "Le directeur doit pouvoir gérer et valider la paie");
        assertTrue(RoleGuard.canViewSalaries(), "Le directeur doit pouvoir consulter les salaires");

        RoleGuard.setCurrentRole(RoleGuard.Role.SECRETARIAT);
        assertFalse(RoleGuard.canManagePayroll(), "Le secrétariat ne peut pas valider la paie");
        assertFalse(RoleGuard.canViewSalaries(), "Le secrétariat ne doit pas accéder aux montants de paie");

        RoleGuard.clearRole();
    }

    @Test
    @DisplayName("Critère 10 : Génération et export PDF du bulletin officiel via Apache PDFBox")
    void testPdfExportOfOfficialPayslip() throws IOException {
        Staff staff = payrollService.findStaffById(1);
        SalaryRecord rec = payrollService.prepareSalary(staff.getId(), "04/2027", 4, 2027,
                SalaryRecord.Frequency.MENSUELLE, 50.0, 25.0, 10.0, 0.0);
        payrollService.validateSalary(rec.getId(), "directeur");

        File targetPdf = new File("target/test_bulletin_" + rec.getRecordNumber() + ".pdf");
        File exported = payrollService.exportPayslipPdf(rec.getId(), targetPdf);

        assertNotNull(exported);
        assertTrue(exported.exists(), "Le fichier PDF doit avoir été généré");
        assertTrue(exported.length() > 500, "Le fichier PDF doit avoir un contenu non vide");
    }
}
