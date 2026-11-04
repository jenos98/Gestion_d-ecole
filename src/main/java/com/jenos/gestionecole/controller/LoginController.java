package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.service.SchoolSettingsService;
import com.jenos.gestionecole.service.SessionManager;
import com.jenos.gestionecole.util.NavigationManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoginController — Contrôleur de l'écran d'authentification.
 *
 * RÈGLES DE SÉCURITÉ :
 * - Aucune information sensible exposée sur l'existence ou non de l'identifiant.
 * - Gestion des états : NORMAL, CHARGEMENT, ERREUR, BLOQUÉ, SUCCÈS.
 * - Connexion via SessionManager.
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnShowPassword;
    @FXML private Button btnLogin;
    @FXML private HBox boxLoading;
    @FXML private ProgressIndicator progressLogin;
    @FXML private Label lblError;
    @FXML private Label lblSchoolName;

    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        // Synchronisation bidirectionnelle du mot de passe clair / masqué
        if (txtPasswordVisible != null && txtPassword != null) {
            txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        }

        // Connexion avec la touche Entrée
        if (txtPassword != null) {
            txtPassword.setOnAction(e -> handleLogin());
        }
        if (txtPasswordVisible != null) {
            txtPasswordVisible.setOnAction(e -> handleLogin());
        }
        if (txtUsername != null) {
            txtUsername.setOnAction(e -> {
                if (passwordVisible && txtPasswordVisible != null) {
                    txtPasswordVisible.requestFocus();
                } else if (txtPassword != null) {
                    txtPassword.requestFocus();
                }
            });
        }

        // Titre de l'établissement depuis les paramètres
        try {
            String name = SchoolSettingsService.getInstance().getSettings().getSchoolName();
            if (lblSchoolName != null && name != null && !name.isBlank()) {
                lblSchoolName.setText(name);
            }
        } catch (Exception ignored) {}

        setInitialState();
    }

    private void setInitialState() {
        if (btnLogin != null) btnLogin.setDisable(false);
        if (boxLoading != null) {
            boxLoading.setVisible(false);
            boxLoading.setManaged(false);
        }
        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setText("");
        }
    }

    // -------------------------------------------------------------------------
    // ACTIONS
    // -------------------------------------------------------------------------

    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (txtPassword != null && txtPasswordVisible != null) {
            txtPassword.setVisible(!passwordVisible);
            txtPassword.setManaged(!passwordVisible);
            txtPasswordVisible.setVisible(passwordVisible);
            txtPasswordVisible.setManaged(passwordVisible);

            if (passwordVisible) {
                txtPasswordVisible.requestFocus();
                txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
            } else {
                txtPassword.requestFocus();
                txtPassword.positionCaret(txtPassword.getText().length());
            }
        }
        if (btnShowPassword != null) {
            btnShowPassword.setText(passwordVisible ? "🙈 Masquer le mot de passe" : "👁 Afficher le mot de passe");
        }
    }

    @FXML
    public void handleLogin() {
        String username = txtUsername != null ? txtUsername.getText().trim() : "";
        String password = txtPassword != null ? txtPassword.getText() : "";

        // Validation immédiate des champs obligatoires
        if (username.isEmpty()) {
            showError("L'identifiant est obligatoire.");
            if (txtUsername != null) txtUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showError("Le mot de passe est obligatoire.");
            if (txtPassword != null) txtPassword.requestFocus();
            return;
        }

        setLoadingState(true);

        // Authentification via SessionManager
        Platform.runLater(() -> {
            try {
                User user = SessionManager.getInstance().login(username, password);
                navigateToMainLayout(user);
            } catch (SecurityException | IllegalArgumentException e) {
                showError(e.getMessage());
            } catch (Exception e) {
                showError("Erreur de connexion : " + e.getMessage());
            } finally {
                setLoadingState(false);
            }
        });
    }

    // -------------------------------------------------------------------------
    // NAVIGATION VERS L'INTERFACE PRINCIPALE
    // -------------------------------------------------------------------------

    private void navigateToMainLayout(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/MainLayout.fxml"));
            Parent root = loader.load();

            MainLayoutController mainController = loader.getController();
            String schoolName = SchoolSettingsService.getInstance().getSettings().getSchoolName();
            mainController.initUserInfo(user.getFullName(), user.getRole().name(), user.getCashRegisterCode());
            mainController.initSchoolInfo(schoolName);

            Stage stage = NavigationManager.getInstance().getPrimaryStage();
            if (stage != null && stage.getScene() != null) {
                stage.getScene().setRoot(root);
            }
        } catch (Exception e) {
            showError("Impossible de charger l'interface principale.");
            logger.error("Impossible de charger l'interface principale : {}", e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // ÉTATS UI
    // -------------------------------------------------------------------------

    private void setLoadingState(boolean loading) {
        if (btnLogin != null) {
            btnLogin.setDisable(loading);
            btnLogin.setText(loading ? "Connexion en cours..." : "Se connecter");
        }
        if (boxLoading != null) {
            boxLoading.setVisible(loading);
            boxLoading.setManaged(loading);
        }
        if (loading && lblError != null) {
            lblError.setVisible(false);
        }
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
        }
    }
}
