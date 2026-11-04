package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.ReportDocument;
import com.jenos.gestionecole.service.ReportService;
import com.jenos.gestionecole.util.NavigationManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PrintPreviewController — Contrôleur de l'aperçu avant impression et export PDF.
 */
public class PrintPreviewController {

    // ── Barre Supérieure (Zoom & Pages) ──
    @FXML private Button btnZoomOut;
    @FXML private Label lblZoom;
    @FXML private Button btnZoomIn;
    @FXML private Button btnPrevPage;
    @FXML private Label lblPageIndicator;
    @FXML private Button btnNextPage;

    // ── Feuille de Document ──
    @FXML private VBox documentSheet;
    @FXML private Label lblSchoolTitle;
    @FXML private Label lblDocTitle;
    @FXML private Label lblDocMeta;
    @FXML private Label lblFilterSummary;
    @FXML private GridPane gridReportTable;
    @FXML private Label lblTotalUSD;
    @FXML private Label lblTotalCDF;

    // ── Barre Inférieure (Actions) ──
    @FXML private Button btnPortrait;
    @FXML private Button btnLandscape;
    @FXML private Button btnPrint;
    @FXML private Button btnExportExcel;
    @FXML private Button btnExportPdf;
    @FXML private Button btnClose;

    private double currentZoom = 1.0;
    private int currentPage = 1;
    private int totalPages = 1;
    private final ReportService reportService = ReportService.getInstance();
    private ReportDocument document;

    @FXML
    public void initialize() {
        document = reportService.getLastGeneratedReport();
        renderDocument();
    }

    public void setReportDocument(ReportDocument doc) {
        this.document = doc;
        renderDocument();
    }

    private void renderDocument() {
        if (document == null) return;

        lblSchoolTitle.setText(document.getSchoolName());
        lblDocTitle.setText(document.getTitle());
        lblDocMeta.setText("Édité le " + document.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
                + " par " + document.getGeneratedBy());
        lblFilterSummary.setText(document.getFilterSummary() != null ? document.getFilterSummary() : "");

        lblTotalUSD.setText(document.getTotalUSD() != null ? document.getTotalUSD() : "");
        lblTotalCDF.setText(document.getTotalCDF() != null ? document.getTotalCDF() : "");

        buildTable(document.getHeaders(), document.getRows());
    }

    private void buildTable(List<String> headers, List<List<String>> rows) {
        gridReportTable.getChildren().clear();
        gridReportTable.getColumnConstraints().clear();
        gridReportTable.getRowConstraints().clear();

        if (headers == null || headers.isEmpty()) return;

        int numCols = headers.size();
        for (int i = 0; i < numCols; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / numCols);
            col.setHgrow(Priority.ALWAYS);
            gridReportTable.getColumnConstraints().add(col);
        }

        // Ligne d'en-tête
        for (int c = 0; c < numCols; c++) {
            Label headerLabel = new Label(headers.get(c));
            headerLabel.getStyleClass().add("report-header-cell");
            headerLabel.setMaxWidth(Double.MAX_VALUE);
            gridReportTable.add(headerLabel, c, 0);
        }

        // Lignes de données
        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            for (int c = 0; c < Math.min(row.size(), numCols); c++) {
                Label cellLabel = new Label(row.get(c));
                cellLabel.getStyleClass().add(r % 2 == 0 ? "report-row-cell" : "report-row-cell-alt");
                cellLabel.setMaxWidth(Double.MAX_VALUE);
                gridReportTable.add(cellLabel, c, r + 1);
            }
        }
    }

    @FXML
    private void handleZoomIn() {
        if (currentZoom < 1.6) {
            currentZoom += 0.15;
            applyZoom();
        }
    }

    @FXML
    private void handleZoomOut() {
        if (currentZoom > 0.6) {
            currentZoom -= 0.15;
            applyZoom();
        }
    }

    private void applyZoom() {
        documentSheet.setScaleX(currentZoom);
        documentSheet.setScaleY(currentZoom);
        lblZoom.setText((int) Math.round(currentZoom * 100) + " %");
    }

    @FXML
    private void handlePortrait() {
        documentSheet.setPrefWidth(720);
        documentSheet.setMinHeight(960);
    }

    @FXML
    private void handleLandscape() {
        documentSheet.setPrefWidth(960);
        documentSheet.setMinHeight(720);
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePageIndicator();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePageIndicator();
        }
    }

    private void updatePageIndicator() {
        if (lblPageIndicator != null) {
            lblPageIndicator.setText("Page " + currentPage + " / " + totalPages);
        }
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage <= 1);
        if (btnNextPage != null) btnNextPage.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(documentSheet.getScene().getWindow())) {
            boolean success = job.printPage(documentSheet);
            if (success) {
                job.endJob();
            }
        }
    }

    @FXML
    private void handleExportExcel() {
        if (document == null) return;
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Exporter en CSV / Excel");
        String safeTitle = document.getTitle().toLowerCase().replaceAll("[^a-z0-9_]+", "_");
        chooser.setInitialFileName(safeTitle + ".csv");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier CSV compatible Excel (*.csv)", "*.csv"));
        java.io.File file = chooser.showSaveDialog(documentSheet.getScene().getWindow());
        if (file != null) {
            boolean ok = com.jenos.gestionecole.service.ExportService.getInstance().exportReportDocument(document, file, ".csv");
            if (ok) {
                com.jenos.gestionecole.util.DialogManager.getInstance().showInfo("Export CSV Réussi", "Rapport exporté avec succès :\n" + file.getAbsolutePath());
            } else {
                com.jenos.gestionecole.util.DialogManager.getInstance().showError("Erreur d'exportation", "Impossible d'écrire le fichier CSV", "Vérifiez les permissions.");
            }
        }
    }

    @FXML
    private void handleExportPdf() {
        if (document == null) return;
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Exporter en PDF");
        String safeTitle = document.getTitle().toLowerCase().replaceAll("[^a-z0-9_]+", "_");
        chooser.setInitialFileName(safeTitle + ".pdf");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Document PDF (*.pdf)", "*.pdf"));
        java.io.File file = chooser.showSaveDialog(documentSheet.getScene().getWindow());
        if (file != null) {
            boolean ok = com.jenos.gestionecole.service.ExportService.getInstance().exportReportDocument(document, file, ".pdf");
            if (ok) {
                com.jenos.gestionecole.util.DialogManager.getInstance().showInfo("Export PDF Réussi", "Rapport enregistré avec succès :\n" + file.getAbsolutePath());
            } else {
                com.jenos.gestionecole.util.DialogManager.getInstance().showError("Erreur d'exportation", "Impossible d'écrire le fichier PDF", "Vérifiez les permissions.");
            }
        }
    }

    @FXML
    private void handleClose() {
        NavigationManager.getInstance().goBack();
    }
}
