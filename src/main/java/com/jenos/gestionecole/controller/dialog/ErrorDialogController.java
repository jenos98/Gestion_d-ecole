package com.jenos.gestionecole.controller.dialog;

import com.jenos.gestionecole.model.AppErrorDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

/**
 * ErrorDialogController — Contrôleur de dialogue d'erreur enrichi pour le Bloc 7.
 *
 * RÈGLES STRICTES :
 * 1. Proposer une cause probable et une action corrective claire.
 * 2. Préciser le statut de l'opération (Échouée).
 * 3. Masquer par défaut les détails techniques.
 */
public class ErrorDialogController {

    @FXML private Label lblTitle;
    @FXML private Label lblErrorCode;
    @FXML private Label lblOperationStatus;
    @FXML private Label lblUserMessage;
    @FXML private Label lblProbableCause;
    @FXML private Label lblSuggestedAction;
    @FXML private Label lblTimestamp;
    @FXML private Button btnToggleDetails;
    @FXML private VBox boxDetails;
    @FXML private TextArea txtTechnicalDetails;
    @FXML private Button btnClose;

    private boolean detailsVisible = false;

    @FXML
    public void initialize() {
        if (boxDetails != null) {
            boxDetails.setVisible(false);
            boxDetails.setManaged(false);
        }
    }

    public void setup(AppErrorDTO error) {
        if (error == null) {
            error = new AppErrorDTO("ERR-SYS-000", "Une erreur imprévue est survenue.", "Détails non spécifiés.");
        }
        if (lblErrorCode != null) {
            lblErrorCode.setText("Code : " + error.getCode());
        }
        if (lblOperationStatus != null) {
            lblOperationStatus.setText(error.isOperationFailed() ? "Opération Échouée" : "Avertissement");
            if (!error.isOperationFailed()) {
                lblOperationStatus.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold;");
            }
        }
        if (lblUserMessage != null) {
            lblUserMessage.setText(error.getUserMessage());
        }
        if (lblProbableCause != null) {
            lblProbableCause.setText(error.getProbableCause() != null ? error.getProbableCause() : "Non précisée.");
        }
        if (lblSuggestedAction != null) {
            lblSuggestedAction.setText(error.getSuggestedAction() != null ? error.getSuggestedAction() : "Veuillez contacter le support si le problème persiste.");
        }
        if (lblTimestamp != null && error.getTimestamp() != null) {
            lblTimestamp.setText(error.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        }
        if (txtTechnicalDetails != null) {
            txtTechnicalDetails.setText(error.getTechnicalMessage());
        }
    }

    @FXML
    private void toggleDetails() {
        detailsVisible = !detailsVisible;
        if (boxDetails != null) {
            boxDetails.setVisible(detailsVisible);
            boxDetails.setManaged(detailsVisible);
        }
        if (btnToggleDetails != null) {
            btnToggleDetails.setText(detailsVisible ? "Masquer les détails techniques ▲" : "Détails techniques ▼");
        }
    }

    @FXML
    private void handleClose() {
        if (btnClose != null && btnClose.getScene() != null && btnClose.getScene().getWindow() != null) {
            Stage stage = (Stage) btnClose.getScene().getWindow();
            stage.close();
        }
    }
}
