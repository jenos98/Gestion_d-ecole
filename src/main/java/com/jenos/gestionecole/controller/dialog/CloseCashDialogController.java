package com.jenos.gestionecole.controller.dialog;

import com.jenos.gestionecole.model.CashSession;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * CloseCashDialogController — Contrôleur du dialogue critique de clôture de caisse.
 * Machine à états : Rapport obligatoire généré & imprimé → Comptage physique USD/CDF → Réconciliation → Clôture.
 */
public class CloseCashDialogController {

    // ── Étape 1 : Rapport Journalier ──
    @FXML private VBox stepReportSection;
    @FXML private Label lblReportStatus;
    @FXML private Label lblPrintStatus;
    @FXML private Button btnGenerateReport;
    @FXML private Button btnPrintReport;
    @FXML private Label lblReportWarning;

    // ── Étape 2 : Comptage & Réconciliation ──
    @FXML private VBox stepCountingSection;
    @FXML private Label lblExpectedUSD;
    @FXML private Label lblExpectedCDF;
    @FXML private TextField txtCountedUSD;
    @FXML private TextField txtCountedCDF;
    @FXML private Label lblDiffUSD;
    @FXML private Label lblDiffCDF;
    @FXML private TextArea txtNotes;

    // ── Boutons d'action ──
    @FXML private Button btnCalculateDiff;
    @FXML private Button btnConfirmClosure;
    @FXML private Button btnCancel;

    // ── Clôture exceptionnelle ──
    @FXML private TitledPane paneExceptionalClosure;
    @FXML private TextField txtExceptionReason;
    @FXML private PasswordField txtSupervisorPass;
    @FXML private Button btnForceClosure;

    // ── Notification ──
    @FXML private Label lblStatusMessage;

    private final CashClosureService closureService = CashClosureService.getInstance();
    private CashSession session;

    @FXML
    public void initialize() {
        session = closureService.getCurrentSession();
        setupView();
    }

    private void setupView() {
        if (session == null) {
            showStatus("Aucune session de caisse active trouvée.", true);
            return;
        }

        // Soldes attendus
        lblExpectedUSD.setText(MoneyFormatter.formatUSD(session.getCurrentBalanceUSD()));
        lblExpectedCDF.setText(MoneyFormatter.formatCDF(session.getCurrentBalanceCDF()));

        // États des rapports
        updateReportStatusUI();

        // Protection de la clôture exceptionnelle
        boolean canForce = RoleGuard.canForceCloseCash();
        paneExceptionalClosure.setVisible(canForce);
        paneExceptionalClosure.setManaged(canForce);

        // Réactivité calcul d'écart automatique
        txtCountedUSD.textProperty().addListener((obs, o, n) -> recalculateDifferences());
        txtCountedCDF.textProperty().addListener((obs, o, n) -> recalculateDifferences());
    }

    private void updateReportStatusUI() {
        boolean gen = session.isReportGenerated();
        boolean prt = session.isReportPrinted();

        lblReportStatus.setText(gen ? "Généré ✓" : "Non généré ✗");
        lblReportStatus.getStyleClass().removeAll("text-success", "text-danger");
        lblReportStatus.getStyleClass().add(gen ? "text-success" : "text-danger");

        lblPrintStatus.setText(prt ? "Imprimé ✓" : "Non imprimé ✗");
        lblPrintStatus.getStyleClass().removeAll("text-success", "text-danger");
        lblPrintStatus.getStyleClass().add(prt ? "text-success" : "text-danger");

        btnPrintReport.setDisable(!gen);

        // Bloquer l'étape de comptage tant que le rapport n'est pas imprimé
        boolean readyForCounting = gen && prt;
        stepCountingSection.setDisable(!readyForCounting);
        btnConfirmClosure.setDisable(!readyForCounting);

        if (!readyForCounting) {
            lblReportWarning.setVisible(true);
            lblReportWarning.setManaged(true);
        } else {
            lblReportWarning.setVisible(false);
            lblReportWarning.setManaged(false);
        }
    }

    @FXML
    private void handleGenerateReport() {
        closureService.generateDailyReport();
        showStatus("Rapport journalier généré. Veuillez l'imprimer avant le comptage.", false);
        updateReportStatusUI();
    }

    @FXML
    private void handlePrintReport() {
        closureService.markDailyReportPrinted();
        showStatus("Impression confirmée. Vous pouvez maintenant procéder au comptage physique.", false);
        updateReportStatusUI();
    }

    @FXML
    private void handleCalculateDiff() {
        recalculateDifferences();
    }

    private void recalculateDifferences() {
        try {
            double cUSD = parseDoubleSafe(txtCountedUSD.getText());
            double cCDF = parseDoubleSafe(txtCountedCDF.getText());

            double diffUSD = cUSD - session.getCurrentBalanceUSD();
            double diffCDF = cCDF - session.getCurrentBalanceCDF();

            lblDiffUSD.setText(String.format("%+.2f USD", diffUSD));
            lblDiffCDF.setText(String.format("%+,.0f CDF", diffCDF));

            styleDifference(lblDiffUSD, diffUSD);
            styleDifference(lblDiffCDF, diffCDF);

        } catch (Exception e) {
            // Ignorer la saisie incomplète
        }
    }

    private void styleDifference(Label label, double diff) {
        label.getStyleClass().removeAll("reconciliation-label-ok", "reconciliation-label-diff");
        if (Math.abs(diff) < 0.01) {
            label.getStyleClass().add("reconciliation-label-ok");
        } else {
            label.getStyleClass().add("reconciliation-label-diff");
        }
    }

    private double parseDoubleSafe(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        return Double.parseDouble(val.trim());
    }

    @FXML
    private void handleConfirmClosure() {
        if (!session.isReportPrinted()) {
            showStatus("Action bloquée : Le rapport journalier doit être imprimé !", true);
            return;
        }

        btnConfirmClosure.setDisable(true);
        try {
            double cUSD = parseDoubleSafe(txtCountedUSD.getText());
            double cCDF = parseDoubleSafe(txtCountedCDF.getText());
            String notes = txtNotes.getText() != null ? txtNotes.getText().trim() : "";

            closureService.recordCounting(cUSD, cCDF);
            closureService.finalizeClosure(notes);

            showStatus("Caisse clôturée avec succès !", false);
            btnGenerateReport.setDisable(true);
            btnPrintReport.setDisable(true);

            com.jenos.gestionecole.service.DashboardService.getInstance().notifyDataChanged();

            // Redirection vers le dashboard
            NavigationManager.getInstance().navigate(Route.DASHBOARD);

        } catch (Exception e) {
            btnConfirmClosure.setDisable(false);
            showStatus("Erreur : " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleForceClosure() {
        String reason = txtExceptionReason.getText() != null ? txtExceptionReason.getText().trim() : "";
        String pass = txtSupervisorPass.getText();

        if (reason.isEmpty()) {
            showStatus("Le motif exceptionnel est obligatoire.", true);
            return;
        }
        if (pass.isEmpty()) {
            showStatus("Mot de passe superviseur requis.", true);
            return;
        }

        closureService.forceClosure(reason, "Promoteur");
        showStatus("Clôture exceptionnelle enregistrée.", false);
        NavigationManager.getInstance().navigate(Route.DASHBOARD);
    }

    @FXML
    private void handleCancel() {
        NavigationManager.getInstance().goBack();
    }

    private void showStatus(String msg, boolean isError) {
        lblStatusMessage.setText(msg);
        lblStatusMessage.setVisible(true);
        lblStatusMessage.setManaged(true);
        lblStatusMessage.getStyleClass().removeAll("alert-error-box", "notification-success");
        lblStatusMessage.getStyleClass().add(isError ? "alert-error-box" : "notification-success");
    }
}
