package com.jenos.gestionecole;

import com.jenos.gestionecole.model.Operation;
import com.jenos.gestionecole.model.ReportDocument;
import com.jenos.gestionecole.service.ExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExportModuleTest — Validation automatisée des fonctionnalités d'exportation réelle (CSV et PDF).
 */
public class ExportModuleTest {

    @Test
    @DisplayName("Export CSV : Génère un fichier CSV UTF-8 BOM avec délimiteur point-virgule")
    void testExportToCsv(@TempDir Path tempDir) throws Exception {
        ExportService service = ExportService.getInstance();
        File targetFile = tempDir.resolve("test_export.csv").toFile();

        ReportDocument doc = new ReportDocument();
        doc.setTitle("Rapport Financier Test");
        doc.setSchoolName("Complexe Scolaire Jenos");
        doc.setHeaders(List.of("Réf", "Date", "Montant", "Devise"));
        doc.setRows(List.of(
                List.of("REC-001", "04/11/2026", "150.00", "USD"),
                List.of("REC-002", "04/11/2026", "50000.00", "CDF")
        ));
        doc.setTotalUSD("150.00 USD");
        doc.setTotalCDF("50000.00 CDF");

        boolean result = service.exportReportDocument(doc, targetFile, ".csv");
        assertTrue(result, "L'export CSV doit réussir");
        assertTrue(targetFile.exists(), "Le fichier cible CSV doit exister");
        assertTrue(targetFile.length() > 0, "Le fichier CSV ne doit pas être vide");

        String content = Files.readString(targetFile.toPath());
        assertTrue(content.contains("Rapport Financier Test"), "Le fichier doit contenir le titre");
        assertTrue(content.contains("REC-001"), "Le fichier doit contenir la première ligne");
        assertTrue(content.contains("REC-002"), "Le fichier doit contenir la seconde ligne");
        assertTrue(content.contains("Total USD : 150.00 USD"), "Le fichier doit contenir les totaux");
    }

    @Test
    @DisplayName("Export PDF : Génère un document PDF binaire valide avec Apache PDFBox")
    void testExportToPdf(@TempDir Path tempDir) {
        ExportService service = ExportService.getInstance();
        File targetFile = tempDir.resolve("test_export.pdf").toFile();

        ReportDocument doc = new ReportDocument();
        doc.setTitle("Journal de Caisse Journalier");
        doc.setSchoolName("Complexe Scolaire Jenos");
        doc.setHeaders(List.of("N° Reçu", "Date", "Type", "Montant", "Devise"));
        doc.setRows(List.of(
                List.of("REC-001", "04/11/2026", "PAIEMENT_ELEVE", "100.00", "USD")
        ));

        boolean result = service.exportReportDocument(doc, targetFile, ".pdf");
        assertTrue(result, "L'export PDF doit réussir");
        assertTrue(targetFile.exists(), "Le fichier PDF doit exister");
        assertTrue(targetFile.length() > 100, "Le fichier PDF doit contenir les données binaires PDFBox");
    }

    @Test
    @DisplayName("Export Liste Opérations : Conversion et export d'une liste d'opérations financières")
    void testExportOperationsList(@TempDir Path tempDir) throws Exception {
        ExportService service = ExportService.getInstance();
        File targetFile = tempDir.resolve("operations_history.csv").toFile();

        Operation op = new Operation(1, "REC-2026-TEST", Operation.OperationType.PAIEMENT_ELEVE,
                "Minerval 1er Trimestre", 250.0, "USD", "ESPECES", null,
                "caissiere01", "Caisse Principale 01", LocalDateTime.now(), Operation.OperationStatus.VALIDEE);

        boolean result = service.exportList("Historique des Opérations", List.of(op), targetFile, ".csv");
        assertTrue(result, "L'export de la liste doit réussir");
        assertTrue(targetFile.exists());

        String content = Files.readString(targetFile.toPath());
        assertTrue(content.contains("REC-2026-TEST"));
        assertTrue(content.contains("Minerval 1er Trimestre"));
    }
}
