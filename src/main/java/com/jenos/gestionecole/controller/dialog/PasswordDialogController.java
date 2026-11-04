package com.jenos.gestionecole.controller.dialog;

import com.jenos.gestionecole.util.MoneyVisibilityManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

/**
 * PasswordDialogController - Dialogue de réauthentification.
 *
 * Utilisé pour :
 * - Afficher les montants masqués
 * - Annuler/corriger une opération
 * - Valider une action sensible
 *
 * Ce dialogue NE crée PAS une nouvelle session.
 * Il vérifie simplement le mot de passe de l'utilisateur connecté.
 */
public class PasswordDialogController {

    @FXML private Label lblMessage;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnConfirm;
    @FXML private Button btnCancel;

    // Action à exécuter si le mot de passe est correct
    private Runnable onSuccess;

    @FXML
    public void initialize() {
        txtPassword.setOnAction(e -> handleConfirm());
    }

    /** Configure le dialogue avant affichage. */
    public void setup(String message, Runnable onSuccess) {
        lblMessage.setText(message);
        this.onSuccess = onSuccess;
    }

    @FXML
    private void handleConfirm() {
        String password = txtPassword.getText();
        if (password.isEmpty()) {
            showError("Veuillez entrer votre mot de passe.");
            return;
        }

        // TODO : Appeler AuthService.verifyPassword(password)
        boolean valid = true; // simulation

        if (valid) {
            closeDialog();
            if (onSuccess != null) onSuccess.run();
        } else {
            showError("Mot de passe incorrect.");
            txtPassword.clear();
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    private void closeDialog() {
        Stage stage = (Stage) btnConfirm.getScene().getWindow();
        stage.close();
    }
}
