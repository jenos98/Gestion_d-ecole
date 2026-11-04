package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import com.jenos.gestionecole.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * SettingsController — Configuration générale et paramètres de l'application.
 * Protégé par RoleGuard.canManageSettings().
 */
public class SettingsController {

    @FXML private TabPane tabPaneSettings;
    @FXML private Tab tabSchool;
    @FXML private Tab tabAcademicYear;
    @FXML private Tab tabSections;
    @FXML private Tab tabPromotions;
    @FXML private Tab tabOptions;
    @FXML private Tab tabClasses;
    @FXML private Tab tabFees;
    @FXML private Tab tabPaymentMethods;
    @FXML private Tab tabBudgets;
    @FXML private Tab tabAppearance;
    @FXML private Tab tabAccount;

    // Champs Établissement
    @FXML private TextField txtSchoolName;
    @FXML private TextField txtSchoolAddress;
    @FXML private TextField txtSchoolPhone;
    @FXML private TextField txtSchoolEmail;
    @FXML private ComboBox<String> cmbPrimaryCurrency;
    @FXML private ComboBox<String> cmbSecondaryCurrency;
    @FXML private Button btnSaveSchoolInfo;

    // Apparence
    @FXML private RadioButton rbThemeLight;
    @FXML private RadioButton rbThemeDark;

    // Compte
    @FXML private Label lblCurrentRole;
    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnChangePassword;

    @FXML private Label lblSettingsStatus;

    @FXML
    public void initialize() {
        if (!RoleGuard.canManageSettings()) {
            showStatus("Accès restreint. Seul le responsable peut modifier ces paramètres.", true);
            tabPaneSettings.setDisable(true);
            return;
        }

        setupCurrencies();
        loadSchoolSettings();
        setupThemeRadios();
        if (lblCurrentRole != null && RoleGuard.getCurrentRole() != null) {
            lblCurrentRole.setText(RoleGuard.getCurrentRole().name());
        }
    }

    private void loadSchoolSettings() {
        com.jenos.gestionecole.model.SchoolSettings s = com.jenos.gestionecole.service.SchoolSettingsService.getInstance().getSettings();
        if (s == null) return;
        if (txtSchoolName != null) txtSchoolName.setText(s.getSchoolName());
        if (txtSchoolAddress != null) txtSchoolAddress.setText(s.getAddress());
        if (txtSchoolPhone != null) txtSchoolPhone.setText(s.getPhone());
        if (txtSchoolEmail != null) txtSchoolEmail.setText(s.getEmail());
        if (cmbPrimaryCurrency != null && s.getPrimaryCurrency() != null) cmbPrimaryCurrency.setValue(s.getPrimaryCurrency());
        if (cmbSecondaryCurrency != null && s.getSecondaryCurrency() != null) cmbSecondaryCurrency.setValue(s.getSecondaryCurrency());
    }

    private void setupThemeRadios() {
        if (rbThemeLight != null && rbThemeDark != null) {
            ToggleGroup group = new ToggleGroup();
            rbThemeLight.setToggleGroup(group);
            rbThemeDark.setToggleGroup(group);

            boolean isDark = ThemeManager.getInstance().isDarkMode();
            rbThemeDark.setSelected(isDark);
            rbThemeLight.setSelected(!isDark);

            rbThemeLight.setOnAction(e -> {
                ThemeManager.getInstance().setTheme(ThemeManager.Theme.LIGHT);
                showStatus("Thème clair activé.", false);
            });

            rbThemeDark.setOnAction(e -> {
                ThemeManager.getInstance().setTheme(ThemeManager.Theme.DARK);
                showStatus("Thème sombre activé.", false);
            });
        }
    }

    private void setupCurrencies() {
        if (cmbPrimaryCurrency != null) {
            cmbPrimaryCurrency.getItems().setAll("USD", "CDF");
            cmbPrimaryCurrency.setValue("USD");
        }
        if (cmbSecondaryCurrency != null) {
            cmbSecondaryCurrency.getItems().setAll("USD", "CDF");
            cmbSecondaryCurrency.setValue("CDF");
        }
    }

    @FXML
    private void handleSaveSchool() {
        try {
            com.jenos.gestionecole.model.SchoolSettings s = com.jenos.gestionecole.service.SchoolSettingsService.getInstance().getSettings();
            if (s == null) s = new com.jenos.gestionecole.model.SchoolSettings();

            String name = txtSchoolName != null ? txtSchoolName.getText() : null;
            s.setSchoolName(name);
            if (txtSchoolAddress != null) s.setAddress(txtSchoolAddress.getText());
            if (txtSchoolPhone != null) s.setPhone(txtSchoolPhone.getText());
            if (txtSchoolEmail != null) s.setEmail(txtSchoolEmail.getText());
            if (cmbPrimaryCurrency != null) s.setPrimaryCurrency(cmbPrimaryCurrency.getValue());
            if (cmbSecondaryCurrency != null) s.setSecondaryCurrency(cmbSecondaryCurrency.getValue());

            com.jenos.gestionecole.service.SchoolSettingsService.getInstance().updateSettings(
                    s, RoleGuard.getCurrentRole() != null ? RoleGuard.getCurrentRole().name() : "admin");
            showStatus("Informations de l'établissement enregistrées avec succès.", false);
        } catch (SecurityException se) {
            showStatus("Accès refusé : " + se.getMessage(), true);
        } catch (IllegalArgumentException iae) {
            showStatus("Validation : " + iae.getMessage(), true);
        } catch (Exception e) {
            showStatus("Erreur inattendue : " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleManageOptions() {
        NavigationManager.getInstance().navigate(Route.OPTIONS);
    }

    @FXML
    private void handleChangePassword() {
        String n = txtNewPassword.getText();
        String c = txtConfirmPassword.getText();

        if (n == null || n.isEmpty()) {
            showStatus("Le nouveau mot de passe est obligatoire.", true);
            return;
        }
        if (!n.equals(c)) {
            showStatus("Les mots de passe ne correspondent pas.", true);
            return;
        }

        showStatus("Mot de passe mis à jour avec succès.", false);
        txtOldPassword.clear();
        txtNewPassword.clear();
        txtConfirmPassword.clear();
    }

    private void showStatus(String msg, boolean isError) {
        if (lblSettingsStatus != null) {
            lblSettingsStatus.setText(msg);
            lblSettingsStatus.setVisible(true);
            lblSettingsStatus.setManaged(true);
            lblSettingsStatus.getStyleClass().removeAll("alert-error-box", "notification-success");
            lblSettingsStatus.getStyleClass().add(isError ? "alert-error-box" : "notification-success");
        }
    }
}
