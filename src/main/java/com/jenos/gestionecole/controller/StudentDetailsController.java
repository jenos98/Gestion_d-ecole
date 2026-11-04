package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Student;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

/**
 * StudentDetailsController — Consultation en lecture seule du dossier complet d'un étudiant.
 */
public class StudentDetailsController {

    @FXML private Label lblFullName;
    @FXML private Label lblMatricule;
    @FXML private Label lblStatut;

    @FXML private Label lblSexe;
    @FXML private Label lblDateNaissance;
    @FXML private Label lblLieuNaissance;
    @FXML private Label lblNationalite;

    @FXML private Label lblAnneeScolaire;
    @FXML private Label lblSection;
    @FXML private Label lblPromotion;
    @FXML private Label lblOption;
    @FXML private Label lblClasse;
    @FXML private Label lblDateInscription;

    @FXML private Label lblGuardianName;
    @FXML private Label lblGuardianPhone;
    @FXML private Label lblGuardianRelation;
    @FXML private Label lblGuardianAddress;
    @FXML private Label lblEmail;

    private Student student;

    public void setStudent(Student s) {
        this.student = s;
        populateView();
    }

    private void populateView() {
        if (student == null) return;

        lblFullName.setText(student.getFullName());
        lblMatricule.setText("Matricule : " + student.getMatricule());
        lblStatut.setText(student.getStatut() != null ? student.getStatut().name() : "ACTIF");
        lblStatut.getStyleClass().removeAll("badge-success", "badge-danger", "badge-warning");
        if (student.getStatut() == Student.StudentStatus.INACTIF) {
            lblStatut.getStyleClass().add("badge-danger");
        } else if (student.getStatut() == Student.StudentStatus.ARCHIVE) {
            lblStatut.getStyleClass().add("badge-warning");
        } else {
            lblStatut.getStyleClass().add("badge-success");
        }

        lblSexe.setText(student.getSexe() != null ? student.getSexe() : "—");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        lblDateNaissance.setText(student.getDateNaissance() != null ? student.getDateNaissance().format(dtf) : "—");
        lblLieuNaissance.setText(student.getLieuNaissance() != null ? student.getLieuNaissance() : "—");
        lblNationalite.setText(student.getNationalite() != null ? student.getNationalite() : "Congolaise");

        lblAnneeScolaire.setText(student.getAnneeScolaire() != null ? student.getAnneeScolaire() : "—");
        lblSection.setText(student.getSectionName());
        lblPromotion.setText(student.getPromotionName());
        lblOption.setText(student.getOptionName());
        lblClasse.setText(student.getClassName());
        lblDateInscription.setText(student.getDateInscription() != null ? student.getDateInscription().format(dtf) : "—");

        lblGuardianName.setText(student.getGuardianName() != null ? student.getGuardianName() : "—");
        lblGuardianPhone.setText(student.getGuardianPhone() != null ? student.getGuardianPhone() : "—");
        lblGuardianRelation.setText(student.getGuardianRelation() != null ? student.getGuardianRelation() : "—");
        lblGuardianAddress.setText(student.getGuardianAddress() != null ? student.getGuardianAddress() : (student.getAdresse() != null ? student.getAdresse() : "—"));
        lblEmail.setText(student.getEmail() != null ? student.getEmail() : "—");
    }

    @FXML
    private void handleEdit() {
        if (student == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student-form.fxml"));
            Node node = loader.load();
            StudentFormController ctrl = loader.getController();
            ctrl.setStudent(student);

            NavigationManager.getInstance().getContentContainer().getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleHistory() {
        NavigationManager.getInstance().navigate(Route.HISTORY);
    }

    @FXML
    private void handleBack() {
        NavigationManager.getInstance().navigate(Route.STUDENTS);
    }
}
