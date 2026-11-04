package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Staff;
import com.jenos.gestionecole.service.StaffService;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * StaffFormController — Contrôleur de création et d'édition d'une fiche employé.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class StaffFormController {

    @FXML private Label lblTitle;
    @FXML private Label lblMatriculeBadge;
    @FXML private Label lblMessage;
    @FXML private Button btnBack;

    // 1. Identité
    @FXML private TextField txtNom;
    @FXML private TextField txtPostnom;
    @FXML private TextField txtPrenom;
    @FXML private ComboBox<String> cmbGenre;
    @FXML private TextField txtQualification;
    @FXML private DatePicker dpDateEmbauche;

    // 2. Affectation & Contact
    @FXML private ComboBox<String> cmbCategorie;
    @FXML private TextField txtFonction;
    @FXML private TextField txtPoste;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtAdresse;

    // 3. Rémunération & Statut
    @FXML private VBox cardSalary;
    @FXML private TextField txtSalaireBase;
    @FXML private ComboBox<String> cmbDevise;
    @FXML private ComboBox<Staff.StaffStatus> cmbStatut;

    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    private final StaffService staffService = StaffService.getInstance();
    private Staff staffToEdit;

    @FXML
    public void initialize() {
        setupComboBoxes();
        applyPermissions();
    }

    private void setupComboBoxes() {
        cmbGenre.setItems(FXCollections.observableArrayList("M", "F"));
        cmbGenre.setValue("M");

        cmbCategorie.setItems(FXCollections.observableArrayList(
                "Enseignant", "Administratif", "Direction", "Ouvrier", "Technique"));
        cmbCategorie.setValue("Enseignant");

        cmbDevise.setItems(FXCollections.observableArrayList("USD", "CDF"));
        cmbDevise.setValue("USD");

        cmbStatut.setItems(FXCollections.observableArrayList(Staff.StaffStatus.values()));
        cmbStatut.setValue(Staff.StaffStatus.ACTIF);

        dpDateEmbauche.setValue(LocalDate.now());
    }

    private void applyPermissions() {
        boolean canViewSalary = RoleGuard.canViewSalaries();
        if (cardSalary != null) {
            cardSalary.setVisible(canViewSalary);
            cardSalary.setManaged(canViewSalary);
        }
    }

    public void setStaff(Staff staff) {
        this.staffToEdit = staff;
        if (staff == null) {
            lblTitle.setText("Nouveau Membre du Personnel");
            lblMatriculeBadge.setText("Nouveau");
            return;
        }

        lblTitle.setText("Modifier la Fiche : " + staff.getFullName());
        lblMatriculeBadge.setText(staff.getMatricule());

        txtNom.setText(staff.getNom());
        txtPostnom.setText(staff.getPostnom());
        txtPrenom.setText(staff.getPrenom());
        cmbGenre.setValue(staff.getGenre() != null ? staff.getGenre() : "M");
        txtQualification.setText(staff.getQualification());
        if (staff.getDateEmbauche() != null) {
            dpDateEmbauche.setValue(staff.getDateEmbauche());
        }

        cmbCategorie.setValue(staff.getCategorie());
        txtFonction.setText(staff.getFonction());
        txtPoste.setText(staff.getPoste());
        txtTelephone.setText(staff.getTelephone());
        txtEmail.setText(staff.getEmail());
        txtAdresse.setText(staff.getAdresse());

        txtSalaireBase.setText(String.valueOf(staff.getSalaireBase()));
        cmbDevise.setValue(staff.getDeviseSalaire() != null ? staff.getDeviseSalaire() : "USD");
        cmbStatut.setValue(staff.getStatut() != null ? staff.getStatut() : Staff.StaffStatus.ACTIF);

        btnSave.setText("Mettre à jour la Fiche");
    }

    @FXML
    private void handleSave() {
        hideMessage();

        String nom = txtNom.getText() != null ? txtNom.getText().trim() : "";
        String postnom = txtPostnom.getText() != null ? txtPostnom.getText().trim() : "";
        String prenom = txtPrenom.getText() != null ? txtPrenom.getText().trim() : "";

        if (nom.isEmpty() || postnom.isEmpty()) {
            showMessage("Le nom et le postnom sont obligatoires.", true);
            return;
        }

        double salaire = 0.0;
        if (RoleGuard.canViewSalaries()) {
            String strSalaire = txtSalaireBase.getText() != null ? txtSalaireBase.getText().trim().replace(",", ".") : "0";
            try {
                salaire = Double.parseDouble(strSalaire);
                if (salaire < 0) {
                    showMessage("Le salaire ne peut pas être négatif.", true);
                    return;
                }
            } catch (NumberFormatException e) {
                showMessage("Montant de salaire invalide.", true);
                return;
            }
        } else if (staffToEdit != null) {
            salaire = staffToEdit.getSalaireBase();
        }

        if (btnSave != null) btnSave.setDisable(true);
        try {
            Staff s = (staffToEdit != null) ? staffToEdit : new Staff();
            s.setNom(nom);
            s.setPostnom(postnom);
            s.setPrenom(prenom);
            s.setGenre(cmbGenre.getValue());
            s.setQualification(txtQualification.getText() != null ? txtQualification.getText().trim() : "");
            s.setDateEmbauche(dpDateEmbauche.getValue());

            s.setCategorie(cmbCategorie.getValue());
            s.setFonction(txtFonction.getText() != null ? txtFonction.getText().trim() : "");
            s.setPoste(txtPoste.getText() != null ? txtPoste.getText().trim() : "");
            s.setTelephone(txtTelephone.getText() != null ? txtTelephone.getText().trim() : "");
            s.setEmail(txtEmail.getText() != null ? txtEmail.getText().trim() : "");
            s.setAdresse(txtAdresse.getText() != null ? txtAdresse.getText().trim() : "");

            s.setSalaireBase(salaire);
            s.setDeviseSalaire(cmbDevise.getValue());
            s.setStatut(cmbStatut.getValue());

            staffService.save(s);

            NavigationManager.getInstance().navigate(Route.STAFF);

        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), true);
        } finally {
            if (btnSave != null) btnSave.setDisable(false);
        }
    }

    @FXML
    private void handleCancel() {
        NavigationManager.getInstance().navigate(Route.STAFF);
    }

    @FXML
    private void handleBack() {
        NavigationManager.getInstance().navigate(Route.STAFF);
    }

    private void showMessage(String msg, boolean isError) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
        lblMessage.getStyleClass().removeAll("alert-error-box", "notification-success");
        lblMessage.getStyleClass().add(isError ? "alert-error-box" : "notification-success");
    }

    private void hideMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
