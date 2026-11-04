package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.SchoolSettings;
import com.jenos.gestionecole.service.SchoolSettingsService;
import com.jenos.gestionecole.util.RoleGuard;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * SchoolSettingsController — Contrôleur de la vue des paramètres d'établissement (school-settings.fxml).
 */
public class SchoolSettingsController {

    @FXML private Label lblStatus;
    @FXML private TextField txtSchoolName;
    @FXML private TextField txtLogoPath;
    @FXML private Button btnBrowseLogo;
    @FXML private TextField txtAddress;
    @FXML private TextField txtCity;
    @FXML private TextField txtCountry;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;

    @FXML private ComboBox<String> cmbPrimaryCurrency;
    @FXML private ComboBox<String> cmbSecondaryCurrency;
    @FXML private ComboBox<String> cmbAutoHide;
    @FXML private Button btnSaveSettings;

    private final SchoolSettingsService settingsService = SchoolSettingsService.getInstance();

    @FXML
    public void initialize() {
        setupCombos();
        loadSettings();
    }

    private void setupCombos() {
        cmbPrimaryCurrency.setItems(FXCollections.observableArrayList("CDF", "USD"));
        cmbSecondaryCurrency.setItems(FXCollections.observableArrayList("USD", "CDF"));

        cmbAutoHide.setItems(FXCollections.observableArrayList(
                "1 minute",
                "5 minutes",
                "10 minutes",
                "15 minutes",
                "Désactivé"
        ));
    }

    private void loadSettings() {
        SchoolSettings s = settingsService.getSettings();
        if (s == null) return;

        txtSchoolName.setText(s.getSchoolName());
        txtLogoPath.setText(s.getLogoPath() != null ? s.getLogoPath() : "");
        txtAddress.setText(s.getAddress());
        txtCity.setText(s.getCity());
        txtCountry.setText(s.getCountry());
        txtPhone.setText(s.getPhone());
        txtEmail.setText(s.getEmail());

        cmbPrimaryCurrency.setValue(s.getPrimaryCurrency());
        cmbSecondaryCurrency.setValue(s.getSecondaryCurrency());

        int mins = s.getAutoHideMinutes();
        if (mins == 1) cmbAutoHide.setValue("1 minute");
        else if (mins == 5) cmbAutoHide.setValue("5 minutes");
        else if (mins == 10) cmbAutoHide.setValue("10 minutes");
        else if (mins == 15) cmbAutoHide.setValue("15 minutes");
        else cmbAutoHide.setValue("Désactivé");
    }

    @FXML
    private void handleBrowseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner le logo de l'établissement");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        Stage stage = (Stage) btnBrowseLogo.getScene().getWindow();
        File f = chooser.showOpenDialog(stage);
        if (f != null) {
            txtLogoPath.setText(f.getAbsolutePath());
        }
    }

    @FXML
    private void handleSave() {
        if (!RoleGuard.can("school.edit")) {
            showNotice("⛔ Modification refusée : Vous n'avez pas l'autorisation de modifier les paramètres globaux.", false);
            return;
        }

        String name = txtSchoolName.getText() != null ? txtSchoolName.getText().trim() : "";
        if (name.isBlank()) {
            showNotice("Le nom de l'école est obligatoire.", false);
            return;
        }

        int mins = 5;
        String autoVal = cmbAutoHide.getValue();
        if ("1 minute".equalsIgnoreCase(autoVal)) mins = 1;
        else if ("5 minutes".equalsIgnoreCase(autoVal)) mins = 5;
        else if ("10 minutes".equalsIgnoreCase(autoVal)) mins = 10;
        else if ("15 minutes".equalsIgnoreCase(autoVal)) mins = 15;
        else if ("Désactivé".equalsIgnoreCase(autoVal)) mins = 0;

        SchoolSettings updated = new SchoolSettings(
                name,
                txtLogoPath.getText(),
                txtAddress.getText(),
                txtPhone.getText(),
                txtEmail.getText(),
                txtCity.getText(),
                txtCountry.getText(),
                cmbPrimaryCurrency.getValue(),
                cmbSecondaryCurrency.getValue(),
                mins
        );

        settingsService.updateSettings(updated);
        showNotice("✓ Les paramètres de l'établissement ont été enregistrés avec succès.", true);
    }

    private void showNotice(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setVisible(true);
        lblStatus.setManaged(true);
        if (success) {
            lblStatus.setStyle("-fx-background-color: #d1e7dd; -fx-text-fill: #0f5132; -fx-padding: 10; -fx-background-radius: 4;");
        } else {
            lblStatus.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-padding: 10; -fx-background-radius: 4;");
        }
    }
}
