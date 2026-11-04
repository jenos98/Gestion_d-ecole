package com.jenos.gestionecole.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * ConfirmDialogController - Dialogue de confirmation générique.
 *
 * Usage :
 *   ConfirmDialogController ctrl = DialogManager.showConfirm(
 *       "Annuler le paiement",
 *       "Cette opération sera marquée comme annulée. Continuer ?",
 *       confirmed -> { if (confirmed) paymentService.cancel(id); }
 *   );
 */
public class ConfirmDialogController {

    @FXML private Label lblTitle;
    @FXML private Label lblMessage;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

    private Consumer<Boolean> callback;

    @FXML
    public void initialize() {}

    /** Configure le dialogue avant affichage. */
    public void setup(String title, String message, Consumer<Boolean> callback) {
        lblTitle.setText(title);
        lblMessage.setText(message);
        this.callback = callback;
    }

    @FXML
    private void handleConfirm() {
        if (callback != null) callback.accept(true);
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        if (callback != null) callback.accept(false);
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) btnConfirm.getScene().getWindow();
        stage.close();
    }
}
