package com.jenos.gestionecole.controller.dialog;

import com.jenos.gestionecole.model.Operation;
import com.jenos.gestionecole.service.HistoryService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.RoleGuard;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

/**
 * OperationDetailsController — Vue détaillée et contrôle d'annulation / correction d'une opération.
 */
public class OperationDetailsController {

    @FXML private Label lblRef;
    @FXML private Label lblDateTime;
    @FXML private Label lblType;
    @FXML private Label lblDescription;
    @FXML private Label lblAmount;
    @FXML private Label lblPaymentMethod;
    @FXML private Label lblTransactionRef;
    @FXML private Label lblCashier;
    @FXML private Label lblCashRegister;
    @FXML private Label lblStatus;

    // Périmètre Scolaire
    @FXML private VBox boxSchoolInfo;
    @FXML private Label lblBeneficiary;
    @FXML private Label lblMatricule;
    @FXML private Label lblSectionPromo;
    @FXML private Label lblOptionClasse;

    // Panneau d'annulation / correction
    @FXML private VBox boxAuditInfo;
    @FXML private Label lblCancelReason;
    @FXML private Label lblCancelledBy;
    @FXML private Label lblCorrectionNotes;

    // Actions
    @FXML private Button btnCancelOperation;
    @FXML private Button btnCorrectOperation;
    @FXML private Button btnClose;

    // Zone formulaire d'action d'annulation/correction
    @FXML private VBox boxActionForm;
    @FXML private Label lblActionFormTitle;
    @FXML private TextField txtActionReason;
    @FXML private TextField txtNewAmount;
    @FXML private Button btnCancelForm;
    @FXML private Button btnConfirmAction;

    private Operation currentOperation;
    private final HistoryService historyService = HistoryService.getInstance();
    private boolean isCorrectionMode = false;

    @FXML
    public void initialize() {
        boxActionForm.setVisible(false);
        boxActionForm.setManaged(false);
    }

    public void setOperation(Operation op) {
        this.currentOperation = op;
        populateData();
    }

    private void populateData() {
        if (currentOperation == null) return;

        lblRef.setText(currentOperation.getReferenceNumber());
        lblDateTime.setText(currentOperation.getDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        lblType.setText(currentOperation.getType().name());
        lblDescription.setText(currentOperation.getDescription());
        lblAmount.setText(MoneyFormatter.format(currentOperation.getAmount(), currentOperation.getCurrency()));
        lblPaymentMethod.setText(currentOperation.getPaymentMethod());
        lblTransactionRef.setText(currentOperation.getTransactionReference() != null ? currentOperation.getTransactionReference() : "—");
        lblCashier.setText(currentOperation.getCashierUsername());
        lblCashRegister.setText(currentOperation.getCashRegisterName());

        lblStatus.setText(currentOperation.getStatus().name());
        applyStatusStyle(currentOperation.getStatus());

        // Données scolaires
        boolean hasSchoolData = (currentOperation.getType() == Operation.OperationType.PAIEMENT_ELEVE)
                || (currentOperation.getStudentMatricule() != null);
        boxSchoolInfo.setVisible(hasSchoolData);
        boxSchoolInfo.setManaged(hasSchoolData);

        if (hasSchoolData) {
            lblBeneficiary.setText(currentOperation.getBeneficiaryName() != null ? currentOperation.getBeneficiaryName() : "—");
            lblMatricule.setText(currentOperation.getStudentMatricule() != null ? currentOperation.getStudentMatricule() : "—");
            lblSectionPromo.setText(currentOperation.getSectionName() + " / " + currentOperation.getPromotionName());
            lblOptionClasse.setText(currentOperation.getOptionName() + " (" + currentOperation.getClassName() + ")");
        }

        // Audit information
        boolean hasAudit = (currentOperation.getStatus() != Operation.OperationStatus.VALIDEE);
        boxAuditInfo.setVisible(hasAudit);
        boxAuditInfo.setManaged(hasAudit);

        if (hasAudit) {
            lblCancelReason.setText("Motif : " + (currentOperation.getCancelReason() != null ? currentOperation.getCancelReason() : "—"));
            lblCancelledBy.setText("Auteur : " + (currentOperation.getCancelledBy() != null ? currentOperation.getCancelledBy() : "—"));
            lblCorrectionNotes.setText("Notes : " + (currentOperation.getCorrectionNotes() != null ? currentOperation.getCorrectionNotes() : "—"));
        }

        // Permissions
        boolean canCancel = RoleGuard.canCancelOperation() && currentOperation.getStatus() == Operation.OperationStatus.VALIDEE;
        btnCancelOperation.setVisible(canCancel);
        btnCancelOperation.setManaged(canCancel);

        boolean canCorrect = RoleGuard.canCorrectOperation() && currentOperation.getStatus() == Operation.OperationStatus.VALIDEE;
        btnCorrectOperation.setVisible(canCorrect);
        btnCorrectOperation.setManaged(canCorrect);
    }

    private void applyStatusStyle(Operation.OperationStatus st) {
        if (st == Operation.OperationStatus.VALIDEE) {
            lblStatus.setStyle("-fx-background-color: #d1e7dd; -fx-text-fill: #0f5132; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-font-weight: bold;");
        } else if (st == Operation.OperationStatus.ANNULEE) {
            lblStatus.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-font-weight: bold;");
        } else if (st == Operation.OperationStatus.CORRIGEE) {
            lblStatus.setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #664d03; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleShowCancelForm() {
        isCorrectionMode = false;
        lblActionFormTitle.setText("Annulation de l'opération (Action Irréversible)");
        txtNewAmount.setVisible(false);
        txtNewAmount.setManaged(false);
        txtActionReason.setPromptText("Motif impératif de l'annulation...");
        boxActionForm.setVisible(true);
        boxActionForm.setManaged(true);
    }

    @FXML
    private void handleShowCorrectForm() {
        isCorrectionMode = true;
        lblActionFormTitle.setText("Rectification / Correction du montant");
        txtNewAmount.setVisible(true);
        txtNewAmount.setManaged(true);
        txtNewAmount.setText(String.valueOf(currentOperation.getAmount()));
        txtActionReason.setPromptText("Motif justificatif de la rectification...");
        boxActionForm.setVisible(true);
        boxActionForm.setManaged(true);
    }

    @FXML
    private void handleCancelForm() {
        boxActionForm.setVisible(false);
        boxActionForm.setManaged(false);
    }

    @FXML
    private void handleConfirmAction() {
        String reason = txtActionReason.getText() != null ? txtActionReason.getText().trim() : "";
        if (reason.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Champ obligatoire");
            alert.setHeaderText("Motif obligatoire");
            alert.setContentText("Vous devez obligatoirement préciser le motif.");
            alert.showAndWait();
            return;
        }

        if (isCorrectionMode) {
            try {
                double newAmt = Double.parseDouble(txtNewAmount.getText().trim());
                historyService.correctOperation(currentOperation.getId(), newAmt, reason, "Promoteur");
            } catch (NumberFormatException e) {
                return;
            }
        } else {
            historyService.cancelOperation(currentOperation.getId(), reason, "Promoteur");
        }

        boxActionForm.setVisible(false);
        boxActionForm.setManaged(false);
        populateData();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
