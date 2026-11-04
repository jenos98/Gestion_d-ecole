package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.service.AuditService;
import com.jenos.gestionecole.service.SessionManager;
import com.jenos.gestionecole.service.UserService;
import com.jenos.gestionecole.util.DialogManager;
import com.jenos.gestionecole.util.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProfileController — Contrôleur du profil utilisateur et de la sécurité.
 */
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @FXML private Label lblFeedback;

    // Champs coordonnées
    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtUsername;
    @FXML private TextField txtRole;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtCashRegister;
    @FXML private Button btnSaveDetails;

    // Sécurité mot de passe
    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblPasswordError;
    @FXML private Button btnChangePassword;
    @FXML private Button btnLogout;

    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            currentUser = UserService.getInstance().findByUsername("admin_promoteur");
        }
        populateUserData();
    }

    private void populateUserData() {
        if (currentUser == null) return;

        txtNom.setText(currentUser.getNom());
        txtPrenom.setText(currentUser.getPrenom() + (currentUser.getPostnom() != null ? " " + currentUser.getPostnom() : ""));
        txtUsername.setText(currentUser.getUsername());
        txtRole.setText(currentUser.getRole() != null ? currentUser.getRole().name() : "NON DÉFINI");
        txtPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");

        String cash = currentUser.getCashRegisterCode();
        txtCashRegister.setText(cash != null && !cash.isBlank() ? cash : "Aucune caisse assignée");
    }

    @FXML
    private void handleSaveDetails() {
        if (currentUser == null) return;

        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();

        if (phone.isEmpty()) {
            showFeedback("Le numéro de téléphone est obligatoire.", true);
            return;
        }

        currentUser.setPhone(phone);
        currentUser.setEmail(email);

        try {
            UserService.getInstance().updateUser(currentUser);
            AuditService.getInstance().logAction(currentUser.getUsername(), "MODIFICATION_PROFIL", "User", currentUser.getUsername(), "Mise à jour des coordonnées personnelles");
            showFeedback("Vos coordonnées ont été enregistrées avec succès.", false);
        } catch (Exception e) {
            showFeedback("Erreur lors de l'enregistrement : " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleChangePassword() {
        lblPasswordError.setVisible(false);
        if (currentUser == null) return;

        String oldPass = txtOldPassword.getText();
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showPasswordError("Tous les champs de mot de passe sont obligatoires.");
            return;
        }

        UserService userService = UserService.getInstance();
        if (!userService.verifyPassword(currentUser, oldPass)) {
            showPasswordError("L'ancien mot de passe saisi est incorrect.");
            return;
        }

        if (newPass.length() < 6) {
            showPasswordError("Le nouveau mot de passe doit comporter au moins 6 caractères.");
            return;
        }

        if (newPass.equals(oldPass)) {
            showPasswordError("Le nouveau mot de passe doit être différent de l'actuel.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showPasswordError("La confirmation ne correspond pas au nouveau mot de passe.");
            return;
        }

        try {
            userService.resetPassword(currentUser.getId(), newPass);
            AuditService.getInstance().logAction(currentUser.getUsername(), "CHANGEMENT_MDP_PROFIL", "User", currentUser.getUsername(), "Changement volontaire de mot de passe");

            txtOldPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();

            showFeedback("Votre mot de passe a été modifié avec succès.", false);
        } catch (Exception e) {
            showPasswordError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        DialogManager.getInstance().showConfirm(
                "Déconnexion",
                "Voulez-vous vraiment vous déconnecter de votre session ?",
                confirmed -> {
                    if (confirmed) {
                        SessionManager.getInstance().logout();
                        try {
                            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                                    getClass().getResource("/fxml/login.fxml")
                            );
                            javafx.scene.Parent root = loader.load();
                            if (NavigationManager.getInstance().getPrimaryStage() != null &&
                                NavigationManager.getInstance().getPrimaryStage().getScene() != null) {
                                NavigationManager.getInstance().getPrimaryStage().getScene().setRoot(root);
                            }
                        } catch (Exception e) {
                            logger.error("Erreur lors de la redirection vers l'écran de connexion après changement de mot de passe", e);
                        }
                    }
                }
        );
    }

    private void showFeedback(String message, boolean isError) {
        lblFeedback.setText(message);
        lblFeedback.getStyleClass().removeAll("badge-success", "badge-danger", "badge");
        lblFeedback.getStyleClass().add("badge");
        lblFeedback.getStyleClass().add(isError ? "badge-danger" : "badge-success");
        lblFeedback.setVisible(true);
        lblFeedback.setManaged(true);
    }

    private void showPasswordError(String error) {
        lblPasswordError.setText(error);
        lblPasswordError.setVisible(true);
    }
}
