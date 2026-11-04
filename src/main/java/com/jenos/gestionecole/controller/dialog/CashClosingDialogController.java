package com.jenos.gestionecole.controller.dialog;

import com.jenos.gestionecole.model.CashSession;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.service.CashRegisterStateManager;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * CashClosingDialogController — Contrôleur de la clôture définitive de caisse.
 *
 * RÈGLES MÉTIER :
 * 1. Clôture normale strictement bloquée sans rapport imprimé
 * 2. Comptage physique CDF et USD rigoureusement séparé
 * 3. Motif obligatoire si le rapprochement détecte un écart
 * 4. Confirmation finale et audit
 */
public class CashClosingDialogController {

    // ── Étape 1 : Rapport Journalier ──
    @FXML private Label lblReportGenStatus;
    @FXML private Label lblReportPrintStatus;

    // ── Étape 2 : Comptage CDF ──
    @FXML private Label lblTheoCDF;
    @FXML private TextField txtCountedCDF;
    @FXML private Label lblDiffCDF;
    @FXML private VBox boxReasonCDF;
    @FXML private TextField txtReasonCDF;

    // ── Étape 3 : Comptage USD ──
    @FXML private Label lblTheoUSD;
    @FXML private TextField txtCountedUSD;
    @FXML private Label lblDiffUSD;
    @FXML private VBox boxReasonUSD;
    @FXML private TextField txtReasonUSD;

    // ── Étape 4 : Confirmation & Notes ──
    @FXML private TextArea txtClosureNotes;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirmClosure;

    // ── Clôture Exceptionnelle ──
    @FXML private TitledPane paneExceptionalClosure;
    @FXML private TextField txtSupervisorReason;
    @FXML private Button btnForceClosure;

    private final CashClosureService closureService = CashClosureService.getInstance();
    private final CashRegisterStateManager stateManager = CashRegisterStateManager.getInstance();

    private double theoCDF = 0.0;
    private double theoUSD = 0.0;
    private double currentDiffCDF = 0.0;
    private double currentDiffUSD = 0.0;

    @FXML
    public void initialize() {
        setupData();
        setupListeners();
    }

    private void setupData() {
        CashSession session = closureService.getCurrentSession();
        if (session == null) return;

        // Étape 1 : Statuts du rapport
        boolean gen = session.isReportGenerated() || stateManager.getReportStatus() != CashRegisterStateManager.ReportStatus.NON_GENERE;
        boolean prt = session.isReportPrinted() || stateManager.getReportStatus() == CashRegisterStateManager.ReportStatus.IMPRIME;

        lblReportGenStatus.setText(gen ? "✓ Généré" : "✗ Non généré");
        lblReportGenStatus.setStyle(gen ? "-fx-text-fill: #198754; -fx-font-weight: bold;" : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");

        lblReportPrintStatus.setText(prt ? "✓ Imprimé" : "✗ Non imprimé");
        lblReportPrintStatus.setStyle(prt ? "-fx-text-fill: #198754; -fx-font-weight: bold;" : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");

        // Soldes théoriques attendus
        theoCDF = session.getCurrentBalanceCDF();
        theoUSD = session.getCurrentBalanceUSD();

        lblTheoCDF.setText(MoneyFormatter.formatCDF(theoCDF));
        lblTheoUSD.setText(MoneyFormatter.formatUSD(theoUSD));

        // Protection de la clôture exceptionnelle
        boolean canForce = RoleGuard.canForceCloseCashRegister();
        paneExceptionalClosure.setVisible(canForce);
        paneExceptionalClosure.setManaged(canForce);
    }

    private void setupListeners() {
        txtCountedCDF.textProperty().addListener((obs, o, n) -> recalculateCDF());
        txtCountedUSD.textProperty().addListener((obs, o, n) -> recalculateUSD());
    }

    private void recalculateCDF() {
        String raw = txtCountedCDF.getText() != null ? txtCountedCDF.getText().trim() : "";
        if (raw.isEmpty()) {
            lblDiffCDF.setText("En attente de saisie");
            lblDiffCDF.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14;");
            boxReasonCDF.setVisible(false);
            boxReasonCDF.setManaged(false);
            return;
        }

        try {
            double counted = Double.parseDouble(raw);
            currentDiffCDF = counted - theoCDF;

            if (Math.abs(currentDiffCDF) < 0.01) {
                lblDiffCDF.setText("✓ Conforme (Écart : 0 CDF)");
                lblDiffCDF.setStyle("-fx-text-fill: #198754; -fx-font-size: 14; -fx-font-weight: bold;");
                boxReasonCDF.setVisible(false);
                boxReasonCDF.setManaged(false);
            } else {
                String prefix = currentDiffCDF > 0 ? "+" : "";
                lblDiffCDF.setText(String.format("Écart : %s%,.0f CDF ⚠️", prefix, currentDiffCDF));
                lblDiffCDF.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14; -fx-font-weight: bold;");
                boxReasonCDF.setVisible(true);
                boxReasonCDF.setManaged(true);
            }
        } catch (NumberFormatException e) {
            lblDiffCDF.setText("Montant invalide");
            lblDiffCDF.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14;");
        }
    }

    private void recalculateUSD() {
        String raw = txtCountedUSD.getText() != null ? txtCountedUSD.getText().trim() : "";
        if (raw.isEmpty()) {
            lblDiffUSD.setText("En attente de saisie");
            lblDiffUSD.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14;");
            boxReasonUSD.setVisible(false);
            boxReasonUSD.setManaged(false);
            return;
        }

        try {
            double counted = Double.parseDouble(raw);
            currentDiffUSD = counted - theoUSD;

            if (Math.abs(currentDiffUSD) < 0.01) {
                lblDiffUSD.setText("✓ Conforme (Écart : 0.00 USD)");
                lblDiffUSD.setStyle("-fx-text-fill: #198754; -fx-font-size: 14; -fx-font-weight: bold;");
                boxReasonUSD.setVisible(false);
                boxReasonUSD.setManaged(false);
            } else {
                String prefix = currentDiffUSD > 0 ? "+" : "";
                lblDiffUSD.setText(String.format("Écart : %s%.2f USD ⚠️", prefix, currentDiffUSD));
                lblDiffUSD.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14; -fx-font-weight: bold;");
                boxReasonUSD.setVisible(true);
                boxReasonUSD.setManaged(true);
            }
        } catch (NumberFormatException e) {
            lblDiffUSD.setText("Montant invalide");
            lblDiffUSD.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14;");
        }
    }

    @FXML
    private void handleConfirmClosure() {
        // 1. Règle du rapport obligatoire
        if (!stateManager.isReadyForNormalClosure()) {
            showErrorAlert("Clôture refusée", "Le rapport journalier doit être généré et imprimé avant la clôture normale.");
            return;
        }

        // 2. Validation saisie comptage
        double cCDF = 0.0;
        double cUSD = 0.0;
        try {
            cCDF = Double.parseDouble(txtCountedCDF.getText().trim());
        } catch (Exception e) {
            showErrorAlert("Saisie incomplète", "Veuillez saisir le montant physique réel compté en Francs Congolais (CDF).");
            return;
        }

        try {
            cUSD = Double.parseDouble(txtCountedUSD.getText().trim());
        } catch (Exception e) {
            showErrorAlert("Saisie incomplète", "Veuillez saisir le montant physique réel compté en Dollars US (USD).");
            return;
        }

        final double countedCDF = cCDF;
        final double countedUSD = cUSD;

        // 3. Vérification des motifs obligatoires en cas d'écart
        String reasonCDF = txtReasonCDF.getText() != null ? txtReasonCDF.getText().trim() : "";
        if (Math.abs(currentDiffCDF) >= 0.01 && reasonCDF.isEmpty()) {
            showErrorAlert("Motif obligatoire", "Un écart a été constaté sur la caisse CDF (" + String.format("%,.0f CDF", currentDiffCDF) + "). Le motif de cet écart est obligatoire.");
            return;
        }

        String reasonUSD = txtReasonUSD.getText() != null ? txtReasonUSD.getText().trim() : "";
        if (Math.abs(currentDiffUSD) >= 0.01 && reasonUSD.isEmpty()) {
            showErrorAlert("Motif obligatoire", "Un écart a été constaté sur la caisse USD (" + String.format("%.2f USD", currentDiffUSD) + "). Le motif de cet écart est obligatoire.");
            return;
        }

        // 4. Confirmation finale
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("CONFIRMATION DE CLÔTURE");
        confirmAlert.setHeaderText("Confirmer la clôture définitive de la caisse ?");
        confirmAlert.setContentText(String.format(
                "• Rapport Journalier : ✓ Imprimé\n" +
                "• Caisse CDF : Vérifiée (Compté : %,.0f CDF | Écart : %,.0f CDF)\n" +
                "• Caisse USD : Vérifiée (Compté : %.2f USD | Écart : %.2f USD)\n\n" +
                "La caisse sera définitivement clôturée pour cette session. Aucune nouvelle opération ne sera possible.",
                countedCDF, currentDiffCDF, countedUSD, currentDiffUSD
        ));

        confirmAlert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                stateManager.recordCounting(countedCDF, countedUSD, reasonCDF, reasonUSD);
                stateManager.confirmClosure(txtClosureNotes.getText());

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Caisse Clôturée");
                success.setHeaderText("Clôture effectuée avec succès");
                success.setContentText("La caisse est maintenant fermée. Les soldes finaux et les écarts éventuels ont été consignés dans le registre d'audit.");
                success.showAndWait();

                NavigationManager.getInstance().navigate(Route.CASH_REGISTER);
            }
        });
    }

    @FXML
    private void handleForceClosure() {
        if (!RoleGuard.canForceCloseCashRegister()) {
            showErrorAlert("Accès refusé", "Seul le superviseur ou promoteur peut ordonner une clôture exceptionnelle.");
            return;
        }

        String reason = txtSupervisorReason.getText() != null ? txtSupervisorReason.getText().trim() : "";
        if (reason.isEmpty()) {
            showErrorAlert("Justification requise", "Vous devez impérativement renseigner le motif administratif de la clôture exceptionnelle.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("DÉROGATION EXCEPTIONNELLE");
        confirm.setHeaderText("Forcer la fermeture exceptionnelle de la caisse ?");
        confirm.setContentText("Cette opération dérogatoire sera enregistrée sous votre responsabilité dans l'audit système.\n\nMotif : " + reason);

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                stateManager.forceClosure(reason);

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Caisse Clôturée");
                info.setHeaderText("Clôture exceptionnelle enregistrée");
                info.setContentText("La caisse a été fermée sous dérogation exceptionnelle.");
                info.showAndWait();

                NavigationManager.getInstance().navigate(Route.CASH_REGISTER);
            }
        });
    }

    @FXML
    private void handleCancel() {
        NavigationManager.getInstance().navigate(Route.CASH_REGISTER);
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
