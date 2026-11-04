package com.jenos.gestionecole;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.FeeService;
import com.jenos.gestionecole.service.SchoolFeeService;
import com.jenos.gestionecole.util.RoleGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SchoolFeeModuleTest — Suite de validation automatisée JUnit 5 pour le Bloc 3
 * (Gestion des frais scolaires).
 *
 * Valide rigoureusement les 8 critères du cahier des charges :
 * 1. Tarifs par section et exceptions par classe.
 * 2. Affectation automatique des frais lors de la validation d'inscription.
 * 3. Génération des échéances selon la fréquence configurée.
 * 4. Refus des paiements supérieurs au solde.
 * 5. Réductions et exemptions avec autorisation.
 * 6. Conservation de l'historique (immutabilité).
 * 7. Reçus et rapports cohérents avec les données.
 * 8. Séparation stricte des monnaies USD / CDF.
 */
public class SchoolFeeModuleTest {

    private SchoolFeeService feeService;
    private Student studentSecondaire;
    private Student studentPrimaire;

    @BeforeEach
    void setUp() {
        // Réinitialiser le service pour chaque test
        SchoolFeeService.resetInstance();
        feeService = SchoolFeeService.getInstance();

        // ── Élève Secondaire Scientifique ──
        // IDs alignés sur ceux des repositories du FeeRepository par défaut (Secondaire = 2, 6e Secondaire = 1)
        Section secSecondaire = new Section(2, "Secondaire");
        Promotion promo6Sec = new Promotion(1, "6e Secondaire", secSecondaire);
        AcademicOption optScientifique = new AcademicOption(1, "Scientifique", promo6Sec);
        Classe classeScA = new Classe(100, "Math-Physique A", optScientifique);

        studentSecondaire = new Student();
        studentSecondaire.setId(1001);
        studentSecondaire.setNom("MUTOMBO");
        studentSecondaire.setPostnom("Kabwe");
        studentSecondaire.setPrenom("Jean");
        studentSecondaire.setMatricule("2025-SEC-T001");
        studentSecondaire.setSexe("M");
        studentSecondaire.setClasse(classeScA);
        studentSecondaire.setAnneeScolaire("2024-2025");

        // ── Élève Primaire ── (Primaire = 1)
        Section secPrimaire = new Section(1, "Primaire");
        Promotion promo3Prim = new Promotion(7, "3e Primaire", secPrimaire);
        Classe classePrim = new Classe(200, "3e Primaire A", promo3Prim);

        studentPrimaire = new Student();
        studentPrimaire.setId(1002);
        studentPrimaire.setNom("ILUNGA");
        studentPrimaire.setPostnom("Kabango");
        studentPrimaire.setPrenom("Marie");
        studentPrimaire.setMatricule("2025-PRI-T002");
        studentPrimaire.setSexe("F");
        studentPrimaire.setClasse(classePrim);
        studentPrimaire.setAnneeScolaire("2024-2025");
    }

    // =========================================================================
    //  Critère 1 : Tarifs par section et exceptions par classe
    // =========================================================================

    @Test
    @DisplayName("Critère 1 : Les tarifs sont définis par section et résolus par la hiérarchie scolaire")
    void testFeeDefinitionBySectionAndClass() {
        FeeService catalogService = FeeService.getInstance();

        // Frais applicables à l'élève Secondaire (GLOBAL + SECTION Secondaire + OPTION Scientifique + PROMO 6e)
        List<Fee> feesSecondaire = catalogService.getFeesForStudent(studentSecondaire);
        assertFalse(feesSecondaire.isEmpty(),
                "L'élève Secondaire Scientifique doit avoir des frais applicables");

        // Doit contenir les frais globaux
        assertTrue(feesSecondaire.stream().anyMatch(f -> f.getScopeLevel() == Fee.ScopeLevel.GLOBAL),
                "Les frais GLOBAL doivent être applicables à tout élève");

        // Doit contenir les frais Section Secondaire (id = 2)
        assertTrue(feesSecondaire.stream().anyMatch(f ->
                        f.getScopeLevel() == Fee.ScopeLevel.SECTION
                                && f.getSection() != null
                                && f.getSection().getId() == 2),
                "Les frais de la Section Secondaire doivent être applicables");

        // Doit contenir les frais Option Scientifique (id = 1)
        assertTrue(feesSecondaire.stream().anyMatch(f ->
                        f.getScopeLevel() == Fee.ScopeLevel.OPTION
                                && f.getOption() != null
                                && f.getOption().getId() == 1),
                "Les frais de l'Option Scientifique doivent être applicables");

        // Ne doit PAS contenir les frais Section Primaire (id = 1)
        assertFalse(feesSecondaire.stream().anyMatch(f ->
                        f.getScopeLevel() == Fee.ScopeLevel.SECTION
                                && f.getSection() != null
                                && f.getSection().getId() == 1),
                "Les frais Primaire ne doivent PAS être applicables à un élève Secondaire");

        // Frais applicables à l'élève Primaire
        List<Fee> feesPrimaire = catalogService.getFeesForStudent(studentPrimaire);
        assertTrue(feesPrimaire.stream().anyMatch(f ->
                        f.getScopeLevel() == Fee.ScopeLevel.SECTION
                                && f.getSection() != null
                                && f.getSection().getId() == 1),
                "Les frais Primaire doivent être applicables à un élève Primaire");

        assertFalse(feesPrimaire.stream().anyMatch(f ->
                        f.getScopeLevel() == Fee.ScopeLevel.SECTION
                                && f.getSection() != null
                                && f.getSection().getId() == 2),
                "Les frais Secondaire ne doivent PAS être applicables à un élève Primaire");
    }

    // =========================================================================
    //  Critère 2 : Affectation automatique lors de la validation d'inscription
    // =========================================================================

    @Test
    @DisplayName("Critère 2 : Les frais sont affectés automatiquement à l'élève lors de l'inscription validée")
    void testAutoAssignmentOnEnrollment() {
        List<StudentFeeAssignment> assigned = feeService.assignFeesToStudent(
                studentSecondaire, "2024-2025", "caissiere01");

        assertFalse(assigned.isEmpty(), "Des frais doivent être affectés");

        // Chaque affectation capture un snapshot
        for (StudentFeeAssignment a : assigned) {
            assertEquals(studentSecondaire.getId(), a.getStudentId());
            assertEquals("2024-2025", a.getAcademicYear());
            assertNotNull(a.getFeeName(), "Le nom du frais doit être capturé");
            assertTrue(a.getOriginalAmount() > 0, "Le montant doit être positif");
            assertNotNull(a.getCurrency(), "La devise doit être capturée");
            assertTrue("USD".equals(a.getCurrency()) || "CDF".equals(a.getCurrency()),
                    "La devise doit être USD ou CDF");
            assertNotNull(a.getInstallments(), "Les échéances doivent être générées");
            assertFalse(a.getInstallments().isEmpty(), "Au moins une échéance doit exister");
            assertEquals(0.0, a.getTotalPaid(), "Aucun paiement initial");
            assertEquals(a.getOriginalAmount(), a.getBalance(), 0.01,
                    "Le solde initial = montant original");
        }

        // Anti-doublon
        assertThrows(IllegalStateException.class,
                () -> feeService.assignFeesToStudent(studentSecondaire, "2024-2025", "caissiere01"),
                "L'affectation en doublon doit être refusée");
    }

    // =========================================================================
    //  Critère 3 : Génération des échéances selon la fréquence configurée
    // =========================================================================

    @Test
    @DisplayName("Critère 3 : Les échéances sont générées selon la fréquence (ANNUEL=1, TRIMESTRIEL=3, MENSUEL=10)")
    void testInstallmentGeneration() {
        // Modifier un frais pour tester TRIMESTRIEL
        Fee feeTrimestriel = FeeService.getInstance().findById(1); // Frais d'Inscription GLOBAL
        assertNotNull(feeTrimestriel);
        double originalAmount = feeTrimestriel.getAmount();
        feeTrimestriel.setFrequency(Fee.PaymentFrequency.TRIMESTRIEL);

        // Modifier un autre frais pour tester MENSUEL
        Fee feeMensuel = FeeService.getInstance().findById(2); // Assurance GLOBAL CDF
        assertNotNull(feeMensuel);
        feeMensuel.setFrequency(Fee.PaymentFrequency.MENSUEL);

        // Affecter les frais
        List<StudentFeeAssignment> assigned = feeService.assignFeesToStudent(
                studentPrimaire, "2024-2025", "agent01");

        // Vérifier le frais TRIMESTRIEL
        StudentFeeAssignment aTrimestriel = assigned.stream()
                .filter(a -> a.getFeeId() == feeTrimestriel.getId())
                .findFirst().orElse(null);
        assertNotNull(aTrimestriel, "L'affectation trimestrielle doit exister");
        assertEquals(3, aTrimestriel.getInstallments().size(),
                "TRIMESTRIEL doit générer exactement 3 échéances");

        // Vérifier que la somme des échéances = montant total
        double sumTrimestriel = aTrimestriel.getInstallments().stream()
                .mapToDouble(FeeInstallment::getAmount).sum();
        assertEquals(originalAmount, sumTrimestriel, 0.01,
                "La somme des échéances trimestrielles doit égaler le montant total");

        // Vérifier le frais MENSUEL
        StudentFeeAssignment aMensuel = assigned.stream()
                .filter(a -> a.getFeeId() == feeMensuel.getId())
                .findFirst().orElse(null);
        assertNotNull(aMensuel, "L'affectation mensuelle doit exister");
        assertEquals(10, aMensuel.getInstallments().size(),
                "MENSUEL doit générer exactement 10 échéances");

        double sumMensuel = aMensuel.getInstallments().stream()
                .mapToDouble(FeeInstallment::getAmount).sum();
        assertEquals(feeMensuel.getAmount(), sumMensuel, 0.01,
                "La somme des échéances mensuelles doit égaler le montant total");

        // Les frais ANNUEL par défaut = 1 échéance
        StudentFeeAssignment aAnnuel = assigned.stream()
                .filter(a -> a.getFeeId() != feeTrimestriel.getId()
                        && a.getFeeId() != feeMensuel.getId())
                .findFirst().orElse(null);
        if (aAnnuel != null) {
            assertEquals(1, aAnnuel.getInstallments().size(),
                    "ANNUEL doit générer exactement 1 échéance");
        }

        // Restaurer les fréquences par défaut
        feeTrimestriel.setFrequency(Fee.PaymentFrequency.ANNUEL);
        feeMensuel.setFrequency(Fee.PaymentFrequency.ANNUEL);
    }

    // =========================================================================
    //  Critère 4 : Refus des paiements supérieurs au solde
    // =========================================================================

    @Test
    @DisplayName("Critère 4 : Un paiement supérieur au solde dû est refusé (anti-trop-perçu)")
    void testOverpaymentRejection() {
        List<StudentFeeAssignment> assigned = feeService.assignFeesToStudent(
                studentSecondaire, "2024-2025", "caissiere01");

        // Prendre le premier frais USD
        StudentFeeAssignment usdAssignment = assigned.stream()
                .filter(a -> "USD".equalsIgnoreCase(a.getCurrency()))
                .findFirst().orElseThrow();

        FeeInstallment installment = usdAssignment.getInstallments().get(0);
        double balance = usdAssignment.getBalance();

        // Payer exactement le montant dû → doit fonctionner
        // D'abord, tenter un montant excessif
        double excessAmount = balance + 100.0;
        assertThrows(IllegalArgumentException.class,
                () -> feeService.recordFeePayment(
                        usdAssignment.getId(), installment.getId(),
                        excessAmount, "USD",
                        Payment.PaymentMethod.ESPECES, null,
                        "caissiere01", studentSecondaire),
                "Un paiement de " + excessAmount + " USD doit être refusé (solde: " + balance + ")");

        // Montant négatif → refusé
        assertThrows(IllegalArgumentException.class,
                () -> feeService.recordFeePayment(
                        usdAssignment.getId(), installment.getId(),
                        -50.0, "USD",
                        Payment.PaymentMethod.ESPECES, null,
                        "caissiere01", studentSecondaire),
                "Un montant négatif doit être refusé");

        // Paiement partiel valide
        double partialAmount = Math.min(10.0, balance);
        Receipt receipt = feeService.recordFeePayment(
                usdAssignment.getId(), installment.getId(),
                partialAmount, "USD",
                Payment.PaymentMethod.ESPECES, null,
                "caissiere01", studentSecondaire);

        assertNotNull(receipt, "Le reçu doit être généré pour un paiement valide");
        assertEquals(balance - partialAmount, usdAssignment.getBalance(), 0.01,
                "Le solde doit diminuer après un paiement partiel valide");
    }

    // =========================================================================
    //  Critère 5 : Réductions et exemptions avec autorisation
    // =========================================================================

    @Test
    @DisplayName("Critère 5 : Les réductions et exemptions requièrent l'autorisation et conservent l'historique")
    void testReductionWithAuthorization() {
        List<StudentFeeAssignment> assigned = feeService.assignFeesToStudent(
                studentSecondaire, "2024-2025", "caissiere01");

        StudentFeeAssignment target = assigned.stream()
                .filter(a -> "USD".equalsIgnoreCase(a.getCurrency()))
                .findFirst().orElseThrow();

        double originalAmount = target.getOriginalAmount();

        // ── CAISSIERE ne peut PAS accorder de réduction ──
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assertThrows(SecurityException.class,
                () -> feeService.grantReduction(target.getId(), 10.0,
                        "Motif test", "caissiere01"),
                "CAISSIERE ne doit pas pouvoir accorder de réduction");

        // ── DIRECTEUR peut accorder une réduction ──
        RoleGuard.setCurrentRole(RoleGuard.Role.DIRECTEUR);
        double reductionAmount = 20.0;
        feeService.grantReduction(target.getId(), reductionAmount,
                "Famille nombreuse", "directeur");

        assertEquals(originalAmount, target.getOriginalAmount(), 0.01,
                "Le montant initial doit être conservé après réduction");
        assertEquals(reductionAmount, target.getReductionAmount(), 0.01,
                "Le montant de la réduction doit être enregistré");
        assertEquals(originalAmount - reductionAmount, target.getAdjustedAmount(), 0.01,
                "Le montant ajusté = initial - réduction");
        assertEquals("Famille nombreuse", target.getReductionMotif(),
                "Le motif doit être conservé");
        assertEquals("directeur", target.getReductionAuthor(),
                "L'auteur doit être conservé");
        assertNotNull(target.getReductionDate(), "La date doit être enregistrée");

        // ── PROMOTEUR peut accorder une exemption ──
        StudentFeeAssignment targetExempt = assigned.stream()
                .filter(a -> a.getId() != target.getId() && "USD".equalsIgnoreCase(a.getCurrency()))
                .findFirst().orElse(null);

        if (targetExempt != null) {
            RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);
            feeService.grantExemption(targetExempt.getId(), "Boursier", "promoteur");

            assertEquals(StudentFeeAssignment.AssignmentStatus.EXEMPTE, targetExempt.getStatus(),
                    "Le statut doit être EXEMPTE");
            assertEquals(0.0, targetExempt.getBalance(), 0.01,
                    "Le solde d'un frais exempté doit être 0");
            assertTrue(targetExempt.getOriginalAmount() > 0,
                    "Le montant initial doit être conservé même après exemption");
        }

        RoleGuard.clearRole();
    }

    // =========================================================================
    //  Critère 6 : Conservation de l'historique (immutabilité)
    // =========================================================================

    @Test
    @DisplayName("Critère 6 : Modifier le tarif catalogue ne modifie pas les frais déjà affectés")
    void testHistoryPreservation() {
        // Affecter les frais
        List<StudentFeeAssignment> assigned = feeService.assignFeesToStudent(
                studentSecondaire, "2024-2025", "caissiere01");

        // Prendre un frais USD affecté et noter son montant snapshot
        StudentFeeAssignment snapshot = assigned.stream()
                .filter(a -> "USD".equalsIgnoreCase(a.getCurrency()))
                .findFirst().orElseThrow();

        double snapshotAmount = snapshot.getOriginalAmount();
        int catalogFeeId = snapshot.getFeeId();

        // Modifier le tarif catalogue
        Fee catalogFee = FeeService.getInstance().findById(catalogFeeId);
        assertNotNull(catalogFee);
        double oldCatalogAmount = catalogFee.getAmount();
        catalogFee.setAmount(oldCatalogAmount + 500.0); // augmentation

        // Vérifier que l'affectation N'A PAS changé
        assertEquals(snapshotAmount, snapshot.getOriginalAmount(), 0.01,
                "Le montant snapshot de l'affectation ne doit PAS changer "
                        + "quand le tarif catalogue est modifié");
        assertEquals(snapshotAmount, snapshot.getBalance(), 0.01,
                "Le solde ne doit PAS être affecté par la modification du catalogue");

        // Restaurer le tarif
        catalogFee.setAmount(oldCatalogAmount);
    }

    // =========================================================================
    //  Critère 7 : Reçus et rapports cohérents avec les données
    // =========================================================================

    @Test
    @DisplayName("Critère 7 : Les reçus et le PDF d'état sont cohérents avec les données enregistrées")
    void testReceiptAndReportCoherence() throws IOException {
        List<StudentFeeAssignment> assigned = feeService.assignFeesToStudent(
                studentSecondaire, "2024-2025", "caissiere01");

        StudentFeeAssignment target = assigned.stream()
                .filter(a -> "USD".equalsIgnoreCase(a.getCurrency()))
                .findFirst().orElseThrow();

        FeeInstallment installment = target.getInstallments().get(0);

        // Paiement partiel
        double paymentAmount = 25.0;
        Receipt receipt = feeService.recordFeePayment(
                target.getId(), installment.getId(),
                paymentAmount, "USD",
                Payment.PaymentMethod.ESPECES, null,
                "caissiere01", studentSecondaire);

        // ── Vérifications du reçu ──
        assertNotNull(receipt);
        assertEquals(paymentAmount, receipt.getAmountPaid(), 0.01,
                "Le reçu doit afficher le montant payé");
        assertEquals("USD", receipt.getCurrency(),
                "Le reçu doit indiquer la bonne devise");
        assertEquals(target.getAdjustedAmount(), receipt.getFeeTotalDue(), 0.01,
                "Le total dû sur le reçu doit correspondre au montant ajusté");
        assertEquals(target.getBalance(), receipt.getBalanceRemaining(), 0.01,
                "Le solde restant sur le reçu doit correspondre au solde réel");

        // ── Vérification de la situation financière ──
        SchoolFeeService.StudentFeeSituation sit =
                feeService.getStudentSituation(studentSecondaire.getId(), "2024-2025");

        assertTrue(sit.totalPaidUSD >= paymentAmount,
                "La situation doit refléter le paiement effectué");
        assertTrue(sit.balanceUSD >= 0,
                "Le solde USD doit être positif ou nul");

        // ── Génération du PDF ──
        File pdf = feeService.generateStudentFeeStatementPdf(
                studentSecondaire.getId(), "2024-2025");

        assertNotNull(pdf, "Le fichier PDF doit être créé");
        assertTrue(pdf.exists(), "Le fichier PDF doit exister sur le disque");
        assertTrue(pdf.length() > 0, "Le fichier PDF ne doit pas être vide");

        // Nettoyage
        pdf.delete();
    }

    // =========================================================================
    //  Critère 8 : Séparation stricte des monnaies USD / CDF
    // =========================================================================

    @Test
    @DisplayName("Critère 8 : Les monnaies USD et CDF restent strictement séparées")
    void testCurrencySeparation() {
        List<StudentFeeAssignment> assigned = feeService.assignFeesToStudent(
                studentSecondaire, "2024-2025", "caissiere01");

        // Vérifier qu'il y a des frais dans les deux devises
        boolean hasUSD = assigned.stream().anyMatch(a -> "USD".equalsIgnoreCase(a.getCurrency()));
        boolean hasCDF = assigned.stream().anyMatch(a -> "CDF".equalsIgnoreCase(a.getCurrency()));
        assertTrue(hasUSD, "L'élève doit avoir des frais en USD");
        assertTrue(hasCDF, "L'élève doit avoir des frais en CDF");

        // ── Tenter de payer un frais USD avec du CDF → refusé ──
        StudentFeeAssignment usdAssignment = assigned.stream()
                .filter(a -> "USD".equalsIgnoreCase(a.getCurrency()))
                .findFirst().orElseThrow();
        FeeInstallment usdInstallment = usdAssignment.getInstallments().get(0);

        assertThrows(IllegalArgumentException.class,
                () -> feeService.recordFeePayment(
                        usdAssignment.getId(), usdInstallment.getId(),
                        10.0, "CDF",
                        Payment.PaymentMethod.ESPECES, null,
                        "caissiere01", studentSecondaire),
                "Payer un frais USD avec du CDF doit être refusé");

        // ── Tenter de payer un frais CDF avec du USD → refusé ──
        StudentFeeAssignment cdfAssignment = assigned.stream()
                .filter(a -> "CDF".equalsIgnoreCase(a.getCurrency()))
                .findFirst().orElseThrow();
        FeeInstallment cdfInstallment = cdfAssignment.getInstallments().get(0);

        assertThrows(IllegalArgumentException.class,
                () -> feeService.recordFeePayment(
                        cdfAssignment.getId(), cdfInstallment.getId(),
                        10.0, "USD",
                        Payment.PaymentMethod.ESPECES, null,
                        "caissiere01", studentSecondaire),
                "Payer un frais CDF avec du USD doit être refusé");

        // ── Vérifier la situation financière : totaux séparés ──
        SchoolFeeService.StudentFeeSituation sit =
                feeService.getStudentSituation(studentSecondaire.getId(), "2024-2025");

        // Les totaux USD et CDF ne doivent pas être mélangés
        double totalUSDAssigned = assigned.stream()
                .filter(a -> "USD".equalsIgnoreCase(a.getCurrency())
                        && a.getStatus() != StudentFeeAssignment.AssignmentStatus.EXEMPTE)
                .mapToDouble(StudentFeeAssignment::getAdjustedAmount)
                .sum();
        double totalCDFAssigned = assigned.stream()
                .filter(a -> "CDF".equalsIgnoreCase(a.getCurrency())
                        && a.getStatus() != StudentFeeAssignment.AssignmentStatus.EXEMPTE)
                .mapToDouble(StudentFeeAssignment::getAdjustedAmount)
                .sum();

        assertEquals(totalUSDAssigned, sit.totalDueUSD, 0.01,
                "Le total USD de la situation doit correspondre aux frais USD affectés");
        assertEquals(totalCDFAssigned, sit.totalDueCDF, 0.01,
                "Le total CDF de la situation doit correspondre aux frais CDF affectés");

        // Aucun mélange
        assertTrue(sit.totalDueUSD != sit.totalDueCDF || sit.totalDueUSD == 0,
                "Les totaux USD et CDF doivent être des valeurs distinctes (pas de conversion)");
    }
}
