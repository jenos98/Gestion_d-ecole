package com.jenos.gestionecole.controller.dialog;

import com.jenos.gestionecole.util.RoleGuard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ExportDialogController — Contrôleur de la boîte de dialogue d'exportation de données (Excel, PDF, CSV).
 * Gère le choix du format, la sélection du répertoire, le nommage normalisé et les autorisations RoleGuard.
 */
public class ExportDialogController {

    @FXML private Label lblReportContext;
    @FXML private ToggleGroup tgFormat;
    @FXML private RadioButton rbExcel;
    @FXML private RadioButton rbPdf;
    @FXML private RadioButton rbCsv;
    @FXML private CheckBox chkFilteredOnly;
    @FXML private Label lblDataCount;
    @FXML private TextField txtFilename;
    @FXML private TextField txtDestination;
    @FXML private Button btnBrowse;
    @FXML private Label lblPermissionNotice;

    private String reportTitle = "Export";
    private List<?> dataToExport = List.of();
    private boolean isAuthorized = true;

    @FXML
    public void initialize() {
        // Destination par défaut : dossier utilisateur ou Documents
        String defaultDir = System.getProperty("user.home") + File.separator + "Documents";
        File dir = new File(defaultDir);
        if (!dir.exists()) {
            defaultDir = System.getProperty("user.home");
        }
        txtDestination.setText(defaultDir);

        // Ajuster l'extension si l'utilisateur change de format
        tgFormat.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updateExtension());
    }

    /**
     * Configure le contexte du document à exporter et applique les contrôles de sécurité RoleGuard.
     */
    public void setupExportContext(String reportTitle, List<?> filteredData) {
        this.reportTitle = (reportTitle != null && !reportTitle.isBlank()) ? reportTitle : "Export_Donnees";
        this.dataToExport = filteredData != null ? filteredData : List.of();

        lblReportContext.setText("Document : " + this.reportTitle);
        lblDataCount.setText("Nombre d'enregistrements : " + this.dataToExport.size());

        // Génération du nom de fichier propre
        String cleanTitle = this.reportTitle.toLowerCase()
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("^_+|_+$", "");
        String dateSuffix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        txtFilename.setText(cleanTitle + "_" + dateSuffix + getSelectedExtension());

        // Contrôle de permission RoleGuard
        checkPermissions();
    }

    private void checkPermissions() {
        String lowerTitle = reportTitle.toLowerCase();
        isAuthorized = true;
        String requiredRole = "";

        if (lowerTitle.contains("opération") || lowerTitle.contains("operation")
                || lowerTitle.contains("caisse") || lowerTitle.contains("financ")
                || lowerTitle.contains("paiement") || lowerTitle.contains("impay")) {
            if (!RoleGuard.canExportFinancialData()) {
                isAuthorized = false;
                requiredRole = "Promoteur uniquement (Données Financières)";
            }
        } else if (lowerTitle.contains("salaire") || lowerTitle.contains("paie")) {
            if (!RoleGuard.canExportSalaryData()) {
                isAuthorized = false;
                requiredRole = "Promoteur uniquement (Données Salariales)";
            }
        } else if (lowerTitle.contains("personnel") || lowerTitle.contains("agent") || lowerTitle.contains("staff")) {
            if (!RoleGuard.canExportStaffData()) {
                isAuthorized = false;
                requiredRole = "Promoteur ou Directeur (Personnel)";
            }
        } else if (lowerTitle.contains("élève") || lowerTitle.contains("eleve") || lowerTitle.contains("student")) {
            if (!RoleGuard.canExportStudentData()) {
                isAuthorized = false;
                requiredRole = "Promoteur, Directeur ou Secrétariat (Élèves)";
            }
        }

        if (!isAuthorized) {
            lblPermissionNotice.setVisible(true);
            lblPermissionNotice.setManaged(true);
            lblPermissionNotice.setText("⛔ Exportation restreinte : vous ne disposez pas des privilèges nécessaires pour exporter ce type de données (" + requiredRole + ").");
        } else {
            lblPermissionNotice.setVisible(false);
            lblPermissionNotice.setManaged(false);
        }
    }

    private String getSelectedExtension() {
        if (rbPdf != null && rbPdf.isSelected()) return ".pdf";
        if (rbCsv != null && rbCsv.isSelected()) return ".csv";
        return ".xlsx";
    }

    private void updateExtension() {
        String current = txtFilename.getText();
        if (current == null || current.isBlank()) return;

        // Remplacement de l'extension existante (.xlsx, .pdf, .csv)
        String baseName = current.replaceAll("\\.(xlsx|pdf|csv)$", "");
        txtFilename.setText(baseName + getSelectedExtension());
    }

    @FXML
    private void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Sélectionner le répertoire de destination");
        File current = new File(txtDestination.getText());
        if (current.exists() && current.isDirectory()) {
            chooser.setInitialDirectory(current);
        }

        Stage stage = (Stage) btnBrowse.getScene().getWindow();
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            txtDestination.setText(selected.getAbsolutePath());
        }
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public String getExportFilePath() {
        String dir = txtDestination.getText().trim();
        String name = txtFilename.getText().trim();
        return dir + File.separator + name;
    }

    public boolean isFilteredOnly() {
        return chkFilteredOnly.isSelected();
    }
}
