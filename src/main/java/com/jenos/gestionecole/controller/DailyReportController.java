package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.CashSession;
import com.jenos.gestionecole.service.CashClosureService;
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
 * DailyReportController — Contrôleur du rapport journalier de caisse obligatoire.
 *
 * RÈGLE FONDAMENTALE :
 * Ce rapport doit être GÉNÉRÉ puis IMPRIMÉ pour débloquer la clôture normale de la caisse.
 */
public class DailyReportController {

    // ── En-tête ──
    @FXML private Label lblReportBadge;
    @FXML private Button btnBackToCash;

    // ── Informations de session ──
    @FXML private Label lblRegisterName;
    @FXML private Label lblCashierName;
    @FXML private Label lblSessionDate;
    @FXML private Label lblOpenTime;

    // ── Réconciliation CDF ──
    @FXML private Label lblInitCDF;
    @FXML private Label lblInCDF;
    @FXML private Label lblOutCDF;
    @FXML private Label lblTheoreticalCDF;

    // ── Réconciliation USD ──
    @FXML private Label lblInitUSD;
    @FXML private Label lblInUSD;
    @FXML private Label lblOutUSD;
    @FXML private Label lblTheoreticalUSD;

    // ── Synthèses ──
    @FXML private Label lblPaymentStats;
    @FXML private Label lblPayCash;
    @FXML private Label lblPayMobile;
    @FXML private Label lblPayBank;

    @FXML private Label lblExpenseStats;
    @FXML private VBox boxExpenseCategories;

    // ── Boutons d'action workflow ──
    @FXML private Label lblReportWorkflowMsg;
    @FXML private Button btnGenerateReport;
    @FXML private Button btnPreviewReport;
    @FXML private Button btnPrintReport;
    @FXML private Button btnProceedToClose;

    private final CashClosureService closureService = CashClosureService.getInstance();
    private final CashRegisterStateManager stateManager = CashRegisterStateManager.getInstance();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        refreshReportData();
    }

    private void refreshReportData() {
        CashSession session = closureService.getCurrentSession();
        if (session == null) return;

        lblRegisterName.setText(session.getCashRegister() != null ? session.getCashRegister().getName() : "Caisse 01");
        lblCashierName.setText(session.getCashierUsername());
        lblSessionDate.setText(session.getOpenedAt() != null ? session.getOpenedAt().toLocalDate().format(df) : "—");
        lblOpenTime.setText(session.getOpenedAt() != null ? session.getOpenedAt().toLocalTime().toString().substring(0, 5) : "—");

        // CDF
        double initCDF = session.getInitialBalanceCDF();
        double inCDF = stateManager.getTotalInCDF();
        double outCDF = stateManager.getTotalOutCDF();
        double theoCDF = initCDF + inCDF - outCDF;

        lblInitCDF.setText(MoneyFormatter.formatCDF(initCDF));
        lblInCDF.setText("+ " + MoneyFormatter.formatCDF(inCDF));
        lblOutCDF.setText("- " + MoneyFormatter.formatCDF(outCDF));
        lblTheoreticalCDF.setText(MoneyFormatter.formatCDF(theoCDF));

        // USD
        double initUSD = session.getInitialBalanceUSD();
        double inUSD = stateManager.getTotalInUSD();
        double outUSD = stateManager.getTotalOutUSD();
        double theoUSD = initUSD + inUSD - outUSD;

        lblInitUSD.setText(MoneyFormatter.formatUSD(initUSD));
        lblInUSD.setText("+ " + MoneyFormatter.formatUSD(inUSD));
        lblOutUSD.setText("- " + MoneyFormatter.formatUSD(outUSD));
        lblTheoreticalUSD.setText(MoneyFormatter.formatUSD(theoUSD));

        // Synthèse modes de paiement
        lblPayCash.setText(String.format("%.2f USD / %,.0f CDF", inUSD, inCDF));
        lblPayMobile.setText("0.00 USD / 0 CDF");
        lblPayBank.setText("0.00 USD / 0 CDF");
        lblPaymentStats.setText("Paiements enregistrés aujourd'hui");

        lblExpenseStats.setText("Dépenses validées aujourd'hui");

        updateWorkflowUI();
    }

    private void updateWorkflowUI() {
        CashRegisterStateManager.ReportStatus status = stateManager.getReportStatus();

        switch (status) {
            case IMPRIME:
                lblReportBadge.setText("RAPPORT IMPRIMÉ ✓");
                lblReportBadge.setStyle("-fx-background-color: #d1e7dd; -fx-text-fill: #0f5132; -fx-padding: 4 10 4 10; -fx-background-radius: 4; -fx-font-weight: bold;");
                lblReportWorkflowMsg.setText("✓ Le rapport journalier a été imprimé avec succès. Vous êtes autorisé(e) à procéder à la clôture.");
                btnGenerateReport.setDisable(true);
                btnPreviewReport.setDisable(false);
                btnPrintReport.setDisable(false);
                btnProceedToClose.setDisable(false);
                break;

            case GENERE:
                lblReportBadge.setText("GÉNÉRÉ (NON IMPRIMÉ)");
                lblReportBadge.setStyle("-fx-background-color: #cff4fc; -fx-text-fill: #055160; -fx-padding: 4 10 4 10; -fx-background-radius: 4; -fx-font-weight: bold;");
                lblReportWorkflowMsg.setText("⚠️ Rapport généré mais non imprimé. Vous devez imprimer le rapport pour débloquer la clôture.");
                btnGenerateReport.setDisable(true);
                btnPreviewReport.setDisable(false);
                btnPrintReport.setDisable(false);
                btnProceedToClose.setDisable(true);
                break;

            default:
                lblReportBadge.setText("NON GÉNÉRÉ");
                lblReportBadge.setStyle("-fx-background-color: #ffe69c; -fx-text-fill: #664d03; -fx-padding: 4 10 4 10; -fx-background-radius: 4; -fx-font-weight: bold;");
                lblReportWorkflowMsg.setText("Rappel : Pour clôturer la caisse, le rapport doit être d'abord GÉNÉRÉ puis IMPRIMÉ.");
                btnGenerateReport.setDisable(false);
                btnPreviewReport.setDisable(true);
                btnPrintReport.setDisable(true);
                btnProceedToClose.setDisable(true);
                break;
        }
    }

    @FXML
    private void handleGenerateReport() {
        CashRegisterStateManager.DailyReportData report = stateManager.generateDailyReport();
        updateWorkflowUI();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rapport généré");
        alert.setHeaderText("Rapport journalier N° " + report.getReportNumber());
        alert.setContentText("Le rapport journalier a été calculé et généré avec succès.\n\nVous devez maintenant prévisualiser ou imprimer le document officiel pour débloquer la clôture.");
        alert.showAndWait();
    }

    @FXML
    private void handlePreviewReport() {
        NavigationManager.getInstance().navigate(Route.DAILY_REPORT_PREVIEW);
    }

    @FXML
    private void handlePrintReport() {
        stateManager.markReportPrinted();
        updateWorkflowUI();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Impression validée");
        alert.setHeaderText("Rapport imprimé avec succès !");
        alert.setContentText("Le statut d'impression a été enregistré dans le journal d'audit.\nLa procédure de clôture normale est maintenant débloquée.");
        alert.showAndWait();
    }

    @FXML
    private void handleProceedToClose() {
        if (!stateManager.isReadyForNormalClosure()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Action bloquée");
            alert.setHeaderText("Impression obligatoire manquante");
            alert.setContentText("Le rapport journalier doit être imprimé avant de clôturer la caisse.");
            alert.showAndWait();
            return;
        }
        NavigationManager.getInstance().navigate(Route.CASH_CLOSING_DIALOG);
    }

    @FXML
    private void handleBackToCash() {
        NavigationManager.getInstance().navigate(Route.CASH_REGISTER);
    }
}
