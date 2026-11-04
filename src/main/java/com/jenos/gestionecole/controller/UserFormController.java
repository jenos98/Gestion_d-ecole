package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.service.UserService;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * UserFormController — Contrôleur du formulaire de création et modification d'utilisateur (user-form.fxml).
 */
public class UserFormController {

    private static User userToEdit;

    @FXML private Label lblFormTitle;
    @FXML private Label lblError;

    @FXML private TextField txtNom;
    @FXML private TextField txtPostnom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtUsername;

    @FXML private ComboBox<RoleGuard.Role> cmbRole;
    @FXML private VBox boxCashRegister;
    @FXML private ComboBox<String> cmbCashRegister;
    @FXML private ComboBox<User.UserStatus> cmbStatus;

    @FXML private VBox boxPasswordFields;
    @FXML private Label lblPassword;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblConfirmPassword;
    @FXML private PasswordField txtConfirmPassword;

    private final UserService userService = UserService.getInstance();

    public static void setUserToEdit(User user) {
        userToEdit = user;
    }

    @FXML
    public void initialize() {
        setupCombos();
        loadUserData();
    }

    private void setupCombos() {
        cmbRole.setItems(FXCollections.observableArrayList(RoleGuard.Role.values()));
        cmbStatus.setItems(FXCollections.observableArrayList(User.UserStatus.values()));
        cmbStatus.setValue(User.UserStatus.ACTIF);

        var registers = CashClosureService.getInstance().getAllRegisters();
        var regNames = FXCollections.observableArrayList("Aucune caisse");
        for (var r : registers) regNames.add(r.getName());
        cmbCashRegister.setItems(regNames);
        cmbCashRegister.setValue("Aucune caisse");

        cmbRole.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCashier = newVal == RoleGuard.Role.CAISSIERE;
            boxCashRegister.setVisible(isCashier);
            boxCashRegister.setManaged(isCashier);
            if (!isCashier) {
                cmbCashRegister.setValue("Aucune caisse");
            }
        });
    }

    private void loadUserData() {
        if (userToEdit != null) {
            lblFormTitle.setText("Modification du Compte : " + userToEdit.getUsername());
            txtNom.setText(userToEdit.getNom());
            txtPostnom.setText(userToEdit.getPostnom());
            txtPrenom.setText(userToEdit.getPrenom());
            txtEmail.setText(userToEdit.getEmail());
            txtPhone.setText(userToEdit.getPhone());
            txtUsername.setText(userToEdit.getUsername());
            txtUsername.setDisable(true); // Identifiant immuable

            cmbRole.setValue(userToEdit.getRole());
            if (userToEdit.getCashRegisterCode() != null) {
                cmbCashRegister.setValue(userToEdit.getCashRegisterCode());
            } else {
                cmbCashRegister.setValue("Aucune caisse");
            }
            cmbStatus.setValue(userToEdit.getStatus());

            // En modification, la saisie du mot de passe est facultative
            lblPassword.setText("Nouveau mot de passe (laisser vide si inchangé)");
            lblConfirmPassword.setText("Confirmer le nouveau mot de passe");
        } else {
            lblFormTitle.setText("Création d'un Compte Utilisateur");
            txtUsername.setDisable(false);
            cmbRole.setValue(RoleGuard.Role.CAISSIERE);
            lblPassword.setText("Mot de passe initial *");
            lblConfirmPassword.setText("Confirmer le mot de passe *");
        }
    }

    @FXML
    private void handleSave() {
        hideError();

        String nom = txtNom.getText() != null ? txtNom.getText().trim() : "";
        String postnom = txtPostnom.getText() != null ? txtPostnom.getText().trim() : "";
        String prenom = txtPrenom.getText() != null ? txtPrenom.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String phone = txtPhone.getText() != null ? txtPhone.getText().trim() : "";
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        RoleGuard.Role role = cmbRole.getValue();
        User.UserStatus status = cmbStatus.getValue();

        String cashRegister = cmbCashRegister.getValue();
        if ("Aucune caisse".equalsIgnoreCase(cashRegister)) cashRegister = null;

        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || username.isBlank() || role == null) {
            showError("Veuillez renseigner tous les champs obligatoires marqués d'un astérisque (*).");
            return;
        }

        if (role == RoleGuard.Role.CAISSIERE && (cashRegister == null || cashRegister.isBlank())) {
            showError("Une caissière doit obligatoirement être associée à une caisse.");
            return;
        }

        String pass = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (userToEdit == null) {
            // Création : mot de passe obligatoire
            if (pass == null || pass.length() < 6) {
                showError("Le mot de passe initial doit contenir au moins 6 caractères.");
                return;
            }
            if (!pass.equals(confirm)) {
                showError("Les deux mots de passe saisis ne sont pas identiques.");
                return;
            }

            try {
                User newUser = new User(0, nom, postnom, prenom, username, email, phone, role, cashRegister, status, null, "");
                userService.createUser(newUser, pass);
                NavigationManager.getInstance().navigate(Route.USERS);
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        } else {
            // Modification
            if (pass != null && !pass.isBlank()) {
                if (pass.length() < 6) {
                    showError("Le nouveau mot de passe doit comporter au moins 6 caractères.");
                    return;
                }
                if (!pass.equals(confirm)) {
                    showError("Les mots de passe ne correspondent pas.");
                    return;
                }
                userService.resetPassword(userToEdit.getId(), pass);
            }

            try {
                userToEdit.setNom(nom);
                userToEdit.setPostnom(postnom);
                userToEdit.setPrenom(prenom);
                userToEdit.setEmail(email);
                userToEdit.setPhone(phone);
                userToEdit.setRole(role);
                userToEdit.setCashRegisterCode(cashRegister);
                userToEdit.setStatus(status);

                userService.updateUser(userToEdit);
                NavigationManager.getInstance().navigate(Route.USERS);
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        userToEdit = null;
        NavigationManager.getInstance().navigate(Route.USERS);
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
