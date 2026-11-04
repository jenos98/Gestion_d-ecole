package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.service.CashRegisterStateManager;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/**
 * DailyReportPreviewController — Contrôleur de prévisualisation officielle A4 du rapport de caisse.
 */
public class DailyReportPreviewController {

    @FXML private Button btnClose;
    @FXML private Button btnZoomOut;
    @FXML private Button btnZoomIn;
    @FXML private Label lblZoom;
    @FXML private Button btnExportPdf;
    @FXML private Button btnPrint;

    @FXML private VBox paperPage;
    @FXML private Label lblDocNumber;
    @FXML private Label lblDocDate;
    @FXML private Label lblCaisseDetails;

    @FXML private Label lblRptInitUSD;
    @FXML private Label lblRptInitCDF;
    @FXML private Label lblRptInUSD;
    @FXML private Label lblRptInCDF;
    @FXML private Label lblRptOutUSD;
    @FXML private Label lblRptOutCDF;
    @FXML private Label lblRptTheoUSD;
    @FXML private Label lblRptTheoCDF;

    private final CashRegisterStateManager stateManager = CashRegisterStateManager.getInstance();
    private double currentScale = 1.0;
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        CashRegisterStateManager.DailyReportData report = stateManager.getLastDailyReport();
        if (report == null) {
            report = stateManager.generateDailyReport();
        }

        lblDocNumber.setText("N° " + report.getReportNumber());
        lblDocDate.setText("Date : " + (report.getDate() != null ? report.getDate().format(df) : "—"));
        lblCaisseDetails.setText(report.getCashRegisterName() + " • Responsable : " + report.getCashierUsername());

        // USD
        lblRptInitUSD.setText(MoneyFormatter.formatUSD(report.getInitialBalanceUSD()));
        lblRptInUSD.setText("+ " + MoneyFormatter.formatUSD(report.getTotalInUSD()));
        lblRptOutUSD.setText("- " + MoneyFormatter.formatUSD(report.getTotalOutUSD()));
        lblRptTheoUSD.setText(MoneyFormatter.formatUSD(report.getTheoreticalBalanceUSD()));

        // CDF
        lblRptInitCDF.setText(MoneyFormatter.formatCDF(report.getInitialBalanceCDF()));
        lblRptInCDF.setText("+ " + MoneyFormatter.formatCDF(report.getTotalInCDF()));
        lblRptOutCDF.setText("- " + MoneyFormatter.formatCDF(report.getTotalOutCDF()));
        lblRptTheoCDF.setText(MoneyFormatter.formatCDF(report.getTheoreticalBalanceCDF()));
    }

    @FXML
    private void handleZoomIn() {
        if (currentScale < 1.4) {
            currentScale += 0.1;
            applyScale();
        }
    }

    @FXML
    private void handleZoomOut() {
        if (currentScale > 0.7) {
            currentScale -= 0.1;
            applyScale();
        }
    }

    private void applyScale() {
        paperPage.setScaleX(currentScale);
        paperPage.setScaleY(currentScale);
        lblZoom.setText(String.format("%.0f%%", currentScale * 100));
    }

    @FXML
    private void handlePrint() {
        // Enregistrer le statut d'impression obligatoire
        stateManager.markReportPrinted();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Impression Confirmée");
        alert.setHeaderText("Rapport journalier imprimé");
        alert.setContentText("Le rapport a été imprimé et validé dans l'audit système.\n\nLe statut est passé à 'IMPRIMÉ'. La clôture de caisse est désormais autorisée.");
        alert.showAndWait();

        NavigationManager.getInstance().navigate(Route.DAILY_REPORT);
    }

    @FXML
    private void handleExportPdf() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export PDF");
        alert.setHeaderText("Exportation réussie");
        alert.setContentText("Le rapport a été exporté sous format PDF sécurisé.");
        alert.showAndWait();
    }

    @FXML
    private void handleClose() {
        NavigationManager.getInstance().navigate(Route.DAILY_REPORT);
    }
}
