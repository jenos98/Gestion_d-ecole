package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.Operation;
import com.jenos.gestionecole.model.ReportDocument;
import com.jenos.gestionecole.model.Student;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ExportService — Service d'exportation réelle des rapports et listes de données.
 * Formats pris en charge : CSV (compatible Excel avec encodage UTF-8 BOM) et PDF via Apache PDFBox.
 */
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private static ExportService instance;

    private ExportService() {}

    public static synchronized ExportService getInstance() {
        if (instance == null) {
            instance = new ExportService();
        }
        return instance;
    }

    /**
     * Exporte un ReportDocument vers le fichier cible selon le format demandé.
     */
    public boolean exportReportDocument(ReportDocument doc, File targetFile, String extension) {
        if (doc == null || targetFile == null) return false;

        try {
            ensureParentDirectoryExists(targetFile);

            if (extension != null && extension.equalsIgnoreCase(".pdf")) {
                return exportToPdf(doc, targetFile);
            } else {
                return exportToCsv(doc, targetFile);
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'export du rapport {} : {}", targetFile.getName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Exporte une liste générique (ex: opérations, élèves) vers le fichier cible.
     */
    public boolean exportList(String title, List<?> data, File targetFile, String extension) {
        ReportDocument doc = convertToReportDocument(title, data);
        return exportReportDocument(doc, targetFile, extension);
    }

    private boolean exportToCsv(ReportDocument doc, File targetFile) {
        try (FileOutputStream fos = new FileOutputStream(targetFile);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {

            // Écriture du BOM UTF-8 pour ouverture directe sans distorsion dans Microsoft Excel
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            // Titre et métadonnées
            writer.write("\"" + escapeCsv(doc.getSchoolName()) + "\"");
            writer.newLine();
            writer.write("\"" + escapeCsv(doc.getTitle()) + "\"");
            writer.newLine();
            if (doc.getFilterSummary() != null && !doc.getFilterSummary().isBlank()) {
                writer.write("\"Filtres: " + escapeCsv(doc.getFilterSummary()) + "\"");
                writer.newLine();
            }
            writer.newLine();

            // En-têtes
            List<String> headers = doc.getHeaders();
            if (headers != null && !headers.isEmpty()) {
                StringBuilder headerLine = new StringBuilder();
                for (int i = 0; i < headers.size(); i++) {
                    if (i > 0) headerLine.append(";");
                    headerLine.append("\"").append(escapeCsv(headers.get(i))).append("\"");
                }
                writer.write(headerLine.toString());
                writer.newLine();
            }

            // Lignes de données
            List<List<String>> rows = doc.getRows();
            if (rows != null) {
                for (List<String> row : rows) {
                    StringBuilder line = new StringBuilder();
                    for (int i = 0; i < row.size(); i++) {
                        if (i > 0) line.append(";");
                        String val = row.get(i) != null ? row.get(i) : "";
                        line.append("\"").append(escapeCsv(val)).append("\"");
                    }
                    writer.write(line.toString());
                    writer.newLine();
                }
            }

            // Totaux
            if (doc.getTotalUSD() != null || doc.getTotalCDF() != null) {
                writer.newLine();
                if (doc.getTotalUSD() != null) {
                    writer.write("\"Total USD : " + escapeCsv(doc.getTotalUSD()) + "\"");
                    writer.newLine();
                }
                if (doc.getTotalCDF() != null) {
                    writer.write("\"Total CDF : " + escapeCsv(doc.getTotalCDF()) + "\"");
                    writer.newLine();
                }
            }

            writer.flush();
            logger.info("Export CSV réussi : {}", targetFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.error("Erreur génération CSV : {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean exportToPdf(ReportDocument doc, File targetFile) {
        try (PDDocument pdfDoc = new PDDocument()) {
            PDRectangle pageSize = doc.isLandscape() ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()) : PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            pdfDoc.addPage(page);

            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(pdfDoc, page)) {
                float y = pageSize.getHeight() - 40;
                float margin = 40;

                // Titre Établissement
                cs.setFont(fontBold, 14);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(sanitizeText(doc.getSchoolName()));
                cs.endText();
                y -= 20;

                // Titre du Document
                cs.setFont(fontBold, 12);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText(sanitizeText(doc.getTitle()));
                cs.endText();
                y -= 16;

                // Date et Auteur
                cs.setFont(fontRegular, 9);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                String meta = "Édité le " + doc.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        + " par " + sanitizeText(doc.getGeneratedBy());
                cs.showText(meta);
                cs.endText();
                y -= 24;

                // En-têtes du tableau
                List<String> headers = doc.getHeaders();
                if (headers != null && !headers.isEmpty()) {
                    float colWidth = (pageSize.getWidth() - (2 * margin)) / Math.max(headers.size(), 1);
                    cs.setFont(fontBold, 9);

                    for (int i = 0; i < headers.size(); i++) {
                        float x = margin + (i * colWidth);
                        cs.beginText();
                        cs.newLineAtOffset(x, y);
                        String headerText = sanitizeText(headers.get(i));
                        if (headerText.length() > 18) headerText = headerText.substring(0, 15) + "...";
                        cs.showText(headerText);
                        cs.endText();
                    }
                    y -= 15;

                    // Lignes
                    cs.setFont(fontRegular, 8);
                    List<List<String>> rows = doc.getRows();
                    if (rows != null) {
                        for (List<String> row : rows) {
                            if (y < 50) break; // Limite de page simple pour la première page
                            for (int i = 0; i < Math.min(row.size(), headers.size()); i++) {
                                float x = margin + (i * colWidth);
                                cs.beginText();
                                cs.newLineAtOffset(x, y);
                                String cell = sanitizeText(row.get(i) != null ? row.get(i) : "");
                                if (cell.length() > 20) cell = cell.substring(0, 17) + "...";
                                cs.showText(cell);
                                cs.endText();
                            }
                            y -= 13;
                        }
                    }
                }
            }

            pdfDoc.save(targetFile);
            logger.info("Export PDF réussi : {}", targetFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.error("Erreur génération PDF : {}", e.getMessage(), e);
            return false;
        }
    }

    private ReportDocument convertToReportDocument(String title, List<?> data) {
        ReportDocument doc = new ReportDocument();
        doc.setTitle(title != null ? title : "Exportation");
        doc.setSchoolName(SchoolSettingsService.getInstance().getSettings() != null
                ? SchoolSettingsService.getInstance().getSettings().getSchoolName()
                : "Complexe Scolaire Jenos");

        if (data == null || data.isEmpty()) {
            doc.setHeaders(List.of("Information"));
            doc.setRows(List.of(List.of("Aucune donnée enregistrée à exporter.")));
            return doc;
        }

        Object first = data.get(0);
        if (first instanceof Operation) {
            doc.setHeaders(List.of("Référence", "Date/Heure", "Type", "Description", "Montant", "Devise", "Caisse", "Statut"));
            List<List<String>> rows = new ArrayList<>();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (Object obj : data) {
                Operation op = (Operation) obj;
                rows.add(List.of(
                        op.getReferenceNumber() != null ? op.getReferenceNumber() : "",
                        op.getDateTime() != null ? op.getDateTime().format(dtf) : "",
                        op.getType() != null ? op.getType().name() : "",
                        op.getDescription() != null ? op.getDescription() : "",
                        String.format("%.2f", op.getAmount()),
                        op.getCurrency() != null ? op.getCurrency() : "",
                        op.getCashRegisterName() != null ? op.getCashRegisterName() : "",
                        op.getStatus() != null ? op.getStatus().name() : ""
                ));
            }
            doc.setRows(rows);
        } else if (first instanceof Student) {
            doc.setHeaders(List.of("Matricule", "Nom", "Post-nom", "Prénom", "Genre", "Classe", "Statut"));
            List<List<String>> rows = new ArrayList<>();
            for (Object obj : data) {
                Student s = (Student) obj;
                rows.add(List.of(
                        s.getMatricule() != null ? s.getMatricule() : "",
                        s.getNom() != null ? s.getNom() : "",
                        s.getPostnom() != null ? s.getPostnom() : "",
                        s.getPrenom() != null ? s.getPrenom() : "",
                        s.getSexe() != null ? s.getSexe() : "",
                        (s.getClasse() != null && s.getClasse().getName() != null) ? s.getClasse().getName() : "",
                        s.getStatut() != null ? s.getStatut().name() : ""
                ));
            }
            doc.setRows(rows);
        } else {
            doc.setHeaders(List.of("Objet", "Détail"));
            List<List<String>> rows = new ArrayList<>();
            for (Object obj : data) {
                rows.add(List.of(obj.getClass().getSimpleName(), obj.toString()));
            }
            doc.setRows(rows);
        }

        return doc;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        // PDFBox Type 1 Helvetica n'accepte que l'encodage ISO-8859-1 (pas de caractères de contrôle bizarres)
        return text.replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", " ").trim();
    }

    private void ensureParentDirectoryExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
