package com.jenos.gestionecole;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.service.HistoryService;
import com.jenos.gestionecole.service.ReportService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.RoleGuard;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HistoryReportsModuleTest — Suite de validation automatisée du Bloc 5 :
 * 1. Filtrage hiérarchique Section → Promotion → Option → Classe dans l'historique
 * 2. Recherche multicritère et par mots-clés dans HistoryService
 * 3. Non-destructivité et traçabilité des opérations (VALIDEE, ANNULEE, CORRIGEE)
 * 4. Masquage de confidentialité des montants (MoneyVisibilityManager)
 * 5. Génération des rapports et séparation stricte USD / CDF
 * 6. Audit trail et traçabilité de génération (ReportLog)
 * 7. Contrôle d'accès et permissions d'exportation (RoleGuard)
 */
public class HistoryReportsModuleTest {

    public static void main(String[] args) {
        System.out.println("=== DÉBUT DES TESTS BLOC 5 (Historique, Rapports & Exports) ===");

        testAcademicCascadeHistorySearch();
        testKeywordAndMultiCriteriaSearch();
        testNonDestructiveOperations();
        testMoneyVisibilityToggle();
        testReportGenerationAndStrictCurrencySeparation();
        testReportAuditLogs();
        testExportRolePermissions();

        System.out.println("=== TOUS LES TESTS DU BLOC 5 ONT RÉUSSI (100%) ===");
    }

    private static void testAcademicCascadeHistorySearch() {
        System.out.print("[TEST 1] Hiérarchie Section → Promotion → Option → Classe (Historique)... ");
        HistoryService history = HistoryService.getInstance();
        AcademicStructureService academic = AcademicStructureService.getInstance();

        List<Section> sections = academic.findAllSections();
        assert !sections.isEmpty() : "Sections académiques requises";

        // Recherche filtrée par Section Secondaire
        List<Operation> secondaryOps = history.search(null, null, null, null, null, null, null, "Secondaire", null, null, null);
        assert !secondaryOps.isEmpty() : "Des opérations de section Secondaire doivent exister";
        for (Operation op : secondaryOps) {
            assert "Secondaire".equalsIgnoreCase(op.getSectionName()) : "Toutes les opérations doivent être du Secondaire";
        }

        // Recherche filtrée par Option "Scientifique" (citoyen de 1er rang)
        List<Operation> scOps = history.search(null, null, null, null, null, null, null, null, null, "Scientifique", null);
        assert !scOps.isEmpty() : "Des opérations de l'option Scientifique doivent exister";
        for (Operation op : scOps) {
            assert "Scientifique".equalsIgnoreCase(op.getOptionName()) : "L'option filtrée doit être Scientifique";
        }

        System.out.println("OK");
    }

    private static void testKeywordAndMultiCriteriaSearch() {
        System.out.print("[TEST 2] Recherche textuelle & multicritères (HistoryService)... ");
        HistoryService history = HistoryService.getInstance();

        // Recherche par mot-clé (nom élève ou référence)
        List<Operation> byKw = history.search(null, null, null, null, null, null, "Kabeya", null, null, null, null);
        assert !byKw.isEmpty() : "Doit retrouver l'élève Kabeya";
        assert byKw.get(0).getBeneficiaryName().contains("KABEYA") : "Le bénéficiaire doit correspondre";

        // Recherche par référence
        List<Operation> byRef = history.search(null, null, null, null, null, null, "REC-2026-0001", null, null, null, null);
        assert !byRef.isEmpty() : "Doit trouver par référence";
        assert "REC-2026-0001".equals(byRef.get(0).getReferenceNumber());

        // Filtrage par type
        List<Operation> expensesOnly = history.search(null, null, Operation.OperationType.DEPENSE, null, null, null, null, null, null, null, null);
        for (Operation op : expensesOnly) {
            assert op.getType() == Operation.OperationType.DEPENSE : "Seules les dépenses doivent ressortir";
        }

        System.out.println("OK");
    }

    private static void testNonDestructiveOperations() {
        System.out.print("[TEST 3] Non-destructivité et traçabilité (Statuts d'opération)... ");
        HistoryService history = HistoryService.getInstance();

        int totalBefore = history.getAllOperations().size();
        assert totalBefore > 0 : "Le journal d'historique ne doit jamais être vide";

        // Annulation d'une opération : statut ANNULEE sans DELETE
        history.cancelOperation(1, "Erreur de saisie", "Directeur");
        assert history.getById(1).getStatus() == Operation.OperationStatus.ANNULEE : "Le statut doit passer à ANNULEE";
        assert "Erreur de saisie".equals(history.getById(1).getCancelReason()) : "Le motif doit être conservé";

        List<Operation> cancelled = history.search(null, null, null, Operation.OperationStatus.ANNULEE, null, null, null, null, null, null, null);
        assert !cancelled.isEmpty() : "Les opérations annulées doivent être conservées dans l'historique pour audit";

        // Correction d'une opération : opération originale passe à CORRIGEE et rectificative créée
        Operation corrective = history.correctOperation(2, 90000.0, "Correction montant", "Promoteur");
        assert history.getById(2).getStatus() == Operation.OperationStatus.CORRIGEE : "L'originale doit être marquée CORRIGEE";
        assert corrective != null && corrective.getAmount() == 90000.0 : "La rectificative doit avoir le nouveau montant";
        assert corrective.getCorrectedOperationId() == 2 : "L'ID originale doit être lié";

        // Aucune opération supprimée physiquement
        assert history.getAllOperations().size() == totalBefore + 1 : "L'historique croît de façon immuable";

        System.out.println("OK");
    }

    private static void testMoneyVisibilityToggle() {
        System.out.print("[TEST 4] Masquage & Révélation des montants de sécurité... ");
        MoneyVisibilityManager mvm = MoneyVisibilityManager.getInstance();

        // Par défaut masqué ou visible
        mvm.hideAll();
        assert !mvm.isVisible() : "Les montants doivent être masqués";
        String masked = MoneyFormatter.display(150.0, "USD");
        assert masked.contains("•") || masked.contains("*") : "L'affichage masqué doit contenir des astérisques ou puces : " + masked;

        mvm.showAll();
        assert mvm.isVisible() : "Les montants doivent être révélés";
        String unmasked = MoneyFormatter.display(150.0, "USD");
        assert unmasked.contains("150") && unmasked.contains("USD") : "L'affichage doit faire apparaître la valeur réelle : " + unmasked;

        System.out.println("OK");
    }

    private static void testReportGenerationAndStrictCurrencySeparation() {
        System.out.print("[TEST 5] Génération de rapport & Séparation stricte USD / CDF... ");
        ReportService reportService = ReportService.getInstance();

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", LocalDate.now().minusDays(30));
        params.put("endDate", LocalDate.now());
        params.put("currency", "Toutes");

        ReportDocument doc = reportService.generateReport(ReportType.RAPPORT_PAIEMENTS, params);
        assert doc != null : "Le document généré ne doit pas être nul";
        assert doc.getTotalUSD() != null && doc.getTotalUSD().contains("USD") : "Total USD obligatoire distinct";
        assert doc.getTotalCDF() != null && doc.getTotalCDF().contains("CDF") : "Total CDF obligatoire distinct";

        // Vérification qu'aucune conversion arbitraire ni somme mélangée n'existe
        assert !doc.getTotalUSD().contains("CDF") : "Le total USD ne doit pas agréger les CDF";
        assert !doc.getTotalCDF().contains("USD") : "Le total CDF ne doit pas agréger les USD";

        System.out.println("OK");
    }

    private static void testReportAuditLogs() {
        System.out.print("[TEST 6] Journalisation d'audit des rapports (ReportLog)... ");
        ReportService reportService = ReportService.getInstance();

        int initialLogs = reportService.getReportLogs().size();
        reportService.generateReport(ReportType.RAPPORT_IMPAYES, Map.of());
        List<ReportLog> logs = reportService.getReportLogs();
        assert logs.size() == initialLogs + 1 : "Chaque génération doit ajouter une entrée au journal d'audit";

        ReportLog lastLog = logs.get(logs.size() - 1);
        assert lastLog.getReportId() != null && !lastLog.getReportId().isBlank() : "L'identifiant du rapport est requis";
        assert lastLog.getGeneratedBy() != null : "L'utilisateur éditeur doit être tracé";
        assert lastLog.getStatus() == ReportLog.ReportStatus.GENERE : "Statut initial GENERE attendu";

        System.out.println("OK");
    }

    private static void testExportRolePermissions() {
        System.out.print("[TEST 7] Permissions RoleGuard pour les exports de données... ");

        // PROMOTEUR: accès complet
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);
        assert RoleGuard.canExportFinancialData() : "Le promoteur peut exporter les données financières";
        assert RoleGuard.canExportStudentData() : "Le promoteur peut exporter les données élèves";
        assert RoleGuard.canExportStaffData() : "Le promoteur peut exporter le personnel";
        assert RoleGuard.canExportSalaryData() : "Le promoteur peut exporter les salaires";

        // CAISSIERE: pas d'export salaires ni export données financières globales
        RoleGuard.setCurrentRole(RoleGuard.Role.CAISSIERE);
        assert !RoleGuard.canExportFinancialData() : "La caissière ne doit pas exporter les données financières globales";
        assert !RoleGuard.canExportSalaryData() : "La caissière ne doit pas exporter les salaires";
        assert !RoleGuard.canExportStaffData() : "La caissière ne doit pas exporter le personnel";

        // DIRECTEUR: élèves et staff oui, mais pas salaires ni finance globale
        RoleGuard.setCurrentRole(RoleGuard.Role.DIRECTEUR);
        assert RoleGuard.canExportStudentData() : "Le directeur peut exporter les élèves";
        assert RoleGuard.canExportStaffData() : "Le directeur peut exporter le personnel";
        assert !RoleGuard.canExportFinancialData() : "Le directeur n'a pas accès à l'export financier global";
        assert !RoleGuard.canExportSalaryData() : "Le directeur n'a pas accès à l'export salarial";

        // Remettre en mode PROMOTEUR pour la suite
        RoleGuard.setCurrentRole(RoleGuard.Role.PROMOTEUR);

        System.out.println("OK");
    }
}
