package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.*;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * EnrollmentController — Contrôleur du flux d'admission officielle et réinscription.
 */
public class EnrollmentController {

    // ── Sélecteur de mode ──
    @FXML private RadioButton rbNewStudent;
    @FXML private RadioButton rbReEnrollment;
    @FXML private ToggleGroup tgEnrollmentMode;

    // ── Panneau Recherche (Réinscription) ──
    @FXML private VBox panelReEnrollLookup;
    @FXML private TextField txtLookupKeyword;
    @FXML private Button btnLookup;
    @FXML private Label lblLookupStudentName;
    @FXML private Label lblLookupStudentDetails;

    // ── Panneau Identité (Nouvelle Inscription) ──
    @FXML private VBox panelNewIdentity;
    @FXML private TextField txtNom;
    @FXML private TextField txtPostnom;
    @FXML private TextField txtPrenom;
    @FXML private ComboBox<String> cmbSexe;
    @FXML private DatePicker dpDateNaissance;
    @FXML private TextField txtLieuNaissance;
    @FXML private TextField txtNationalite;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtAdresse;

    // ── Cursus & Affectation (Cascade) ──
    @FXML private ComboBox<String> cmbAcademicYear;
    @FXML private ComboBox<Section> cmbSection;
    @FXML private ComboBox<Promotion> cmbPromotion;
    @FXML private ComboBox<AcademicOption> cmbOption;
    @FXML private ComboBox<Classe> cmbClasse;
    @FXML private TextField txtMatricule;
    @FXML private Button btnGenerateMatricule;

    // ── Frais & Encaissement d'admission ──
    @FXML private CheckBox chkPayNow;
    @FXML private VBox panelPaymentDetails;
    @FXML private ComboBox<Fee> cmbFee;
    @FXML private TextField txtAmount;
    @FXML private ComboBox<String> cmbCurrency;
    @FXML private ComboBox<Payment.PaymentMethod> cmbPaymentMethod;
    @FXML private TextField txtPaymentReference;

    // ── Alertes & Actions ──
    @FXML private Label lblErrorMessage;
    @FXML private Label lblSuccessMessage;
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;

    // Services
    private final EnrollmentService enrollmentService = EnrollmentService.getInstance();
    private final AcademicStructureService structureService = AcademicStructureService.getInstance();
    private final StudentService studentService = StudentService.getInstance();
    private final PaymentService paymentService = PaymentService.getInstance();

    private Student existingStudentSelected;

    @FXML
    public void initialize() {
        setupModeToggle();
        setupHierarchicalCascade();
        setupPaymentFields();
        clearMessages();
    }

    private void setupModeToggle() {
        tgEnrollmentMode = new ToggleGroup();
        rbNewStudent.setToggleGroup(tgEnrollmentMode);
        rbReEnrollment.setToggleGroup(tgEnrollmentMode);
        rbNewStudent.setSelected(true);

        tgEnrollmentMode.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            boolean isNew = rbNewStudent.isSelected();
            panelNewIdentity.setVisible(isNew);
            panelNewIdentity.setManaged(isNew);
            panelReEnrollLookup.setVisible(!isNew);
            panelReEnrollLookup.setManaged(!isNew);
            clearMessages();
        });

        cmbSexe.setItems(FXCollections.observableArrayList("M", "F"));
        txtNationalite.setText("Congolaise");
    }

    private void setupHierarchicalCascade() {
        cmbAcademicYear.setItems(FXCollections.observableArrayList(structureService.findAllAcademicYears()));
        if (!cmbAcademicYear.getItems().isEmpty()) {
            cmbAcademicYear.setValue(cmbAcademicYear.getItems().get(0));
        }

        cmbSection.setItems(FXCollections.observableArrayList(structureService.findAllSections()));

        cmbSection.valueProperty().addListener((obs, oldVal, newSection) -> {
            cmbPromotion.getItems().clear();
            cmbOption.getItems().clear();
            cmbClasse.getItems().clear();
            if (newSection != null) {
                cmbPromotion.setItems(FXCollections.observableArrayList(
                        structureService.findPromotionsBySection(newSection)
                ));
            }
        });

        cmbPromotion.valueProperty().addListener((obs, oldVal, newPromo) -> {
            cmbOption.getItems().clear();
            cmbClasse.getItems().clear();
            if (newPromo != null) {
                boolean hasOptions = structureService.promotionHasOptions(newPromo);
                cmbOption.setVisible(hasOptions);
                cmbOption.setManaged(hasOptions);

                if (hasOptions) {
                    cmbOption.setItems(FXCollections.observableArrayList(
                            structureService.findOptionsByPromotion(newPromo)
                    ));
                } else {
                    cmbClasse.setItems(FXCollections.observableArrayList(
                            structureService.findClassesByPromotion(newPromo)
                    ));
                }
            }
        });

        cmbOption.valueProperty().addListener((obs, oldVal, newOpt) -> {
            cmbClasse.getItems().clear();
            if (newOpt != null) {
                cmbClasse.setItems(FXCollections.observableArrayList(
                        structureService.findClassesByOption(newOpt)
                ));
            }
        });
    }

    private void setupPaymentFields() {
        chkPayNow.selectedProperty().addListener((obs, o, isChecked) -> {
            panelPaymentDetails.setVisible(isChecked);
            panelPaymentDetails.setManaged(isChecked);
        });

        List<Fee> fees = paymentService.getFeesForStudent(null);
        cmbFee.setItems(FXCollections.observableArrayList(fees));
        if (!fees.isEmpty()) {
            cmbFee.setValue(fees.get(0));
            txtAmount.setText(String.valueOf(fees.get(0).getAmount()));
        }

        cmbFee.valueProperty().addListener((obs, o, f) -> {
            if (f != null) {
                txtAmount.setText(String.valueOf(f.getAmount()));
                cmbCurrency.setValue(f.getCurrency());
            }
        });

        cmbCurrency.setItems(FXCollections.observableArrayList("USD", "CDF"));
        cmbCurrency.setValue("USD");

        cmbPaymentMethod.setItems(FXCollections.observableArrayList(Payment.PaymentMethod.values()));
        cmbPaymentMethod.setValue(Payment.PaymentMethod.ESPECES);
    }

    @FXML
    private void handleLookupStudent() {
        clearMessages();
        String kw = txtLookupKeyword.getText() != null ? txtLookupKeyword.getText().trim() : "";
        if (kw.isEmpty()) {
            showError("Veuillez entrer un matricule ou un nom.");
            return;
        }

        List<Student> results = studentService.searchByKeyword(kw);
        if (results.isEmpty()) {
            showError("Aucun étudiant trouvé pour : " + kw);
            existingStudentSelected = null;
            lblLookupStudentName.setText("—");
            lblLookupStudentDetails.setText("—");
        } else {
            existingStudentSelected = results.get(0);
            lblLookupStudentName.setText(existingStudentSelected.getFullName() + " (" + existingStudentSelected.getMatricule() + ")");
            lblLookupStudentDetails.setText("Classe précédente : " + existingStudentSelected.getClassName() + " | Année : " + existingStudentSelected.getAnneeScolaire());
            txtMatricule.setText(existingStudentSelected.getMatricule());
            txtMatricule.setDisable(true);
        }
    }

    @FXML
    private void handleGenerateMatricule() {
        String year = cmbAcademicYear.getValue();
        Section sec = cmbSection.getValue();
        txtMatricule.setText(studentService.generateMatricule(year, sec));
    }

    @FXML
    private void handleSubmit() {
        clearMessages();
        String year = cmbAcademicYear.getValue();
        Classe classe = cmbClasse.getValue();

        if (classe == null) {
            showError("Veuillez sélectionner la classe d'affectation.");
            return;
        }

        boolean payNow = chkPayNow.isSelected();
        Fee fee = cmbFee.getValue();
        double amount = 0.0;
        if (payNow) {
            try {
                amount = Double.parseDouble(txtAmount.getText().trim());
            } catch (Exception e) {
                showError("Montant des frais invalide.");
                return;
            }
        }

        String curr = cmbCurrency.getValue();
        Payment.PaymentMethod method = cmbPaymentMethod.getValue();
        String ref = txtPaymentReference.getText() != null ? txtPaymentReference.getText().trim() : "";
        String agent = RoleGuard.getCurrentRole() != null ? RoleGuard.getCurrentRole().name() : "Admin";

        if (btnSubmit != null) btnSubmit.setDisable(true);
        try {
            if (rbNewStudent.isSelected()) {
                // Création d'un nouvel étudiant
                Student s = new Student();
                s.setNom(txtNom.getText() != null ? txtNom.getText().trim() : "");
                s.setPostnom(txtPostnom.getText() != null ? txtPostnom.getText().trim() : "");
                s.setPrenom(txtPrenom.getText() != null ? txtPrenom.getText().trim() : "");
                s.setSexe(cmbSexe.getValue());
                s.setDateNaissance(dpDateNaissance.getValue());
                s.setLieuNaissance(txtLieuNaissance.getText() != null ? txtLieuNaissance.getText().trim() : "");
                s.setNationalite(txtNationalite.getText() != null ? txtNationalite.getText().trim() : "Congolaise");
                s.setTelephone(txtTelephone.getText() != null ? txtTelephone.getText().trim() : "");
                s.setEmail(txtEmail.getText() != null ? txtEmail.getText().trim() : "");
                s.setAdresse(txtAdresse.getText() != null ? txtAdresse.getText().trim() : "");
                s.setMatricule(txtMatricule.getText() != null ? txtMatricule.getText().trim() : "");
                s.setSection(cmbSection.getValue());
                s.setPromotion(cmbPromotion.getValue());
                s.setOption(cmbOption.getValue());

                Enrollment enr = enrollmentService.processNewEnrollment(s, classe, year, payNow, fee, amount, curr, method, ref, agent);
                showSuccess("Nouvelle inscription validée avec succès pour " + s.getFullName() + " (Matricule: " + s.getMatricule() + ").");
            } else {
                // Réinscription
                if (existingStudentSelected == null) {
                    showError("Veuillez d'abord rechercher et sélectionner l'étudiant à réinscrire.");
                    if (btnSubmit != null) btnSubmit.setDisable(false);
                    return;
                }
                Enrollment enr = enrollmentService.processReEnrollment(existingStudentSelected, classe, year, payNow, fee, amount, curr, method, ref, agent);
                showSuccess("Réinscription validée avec succès pour " + existingStudentSelected.getFullName() + " en " + classe.getName() + ".");
            }

            com.jenos.gestionecole.service.DashboardService.getInstance().notifyDataChanged();
            if (btnSubmit != null) {
                btnSubmit.setText("Inscrire un autre élève");
                btnSubmit.setDisable(false);
            }
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            if (btnSubmit != null) btnSubmit.setDisable(false);
        } catch (Exception e) {
            showError("Erreur lors de l'inscription : " + e.getMessage());
            if (btnSubmit != null) btnSubmit.setDisable(false);
        }
    }

    @FXML
    private void handleCancel() {
        NavigationManager.getInstance().navigate(Route.STUDENTS);
    }

    private void showError(String msg) {
        lblErrorMessage.setText(msg);
        lblErrorMessage.setVisible(true);
        lblErrorMessage.setManaged(true);
    }

    private void showSuccess(String msg) {
        lblSuccessMessage.setText(msg);
        lblSuccessMessage.setVisible(true);
        lblSuccessMessage.setManaged(true);
    }

    private void clearMessages() {
        lblErrorMessage.setVisible(false);
        lblErrorMessage.setManaged(false);
        lblSuccessMessage.setVisible(false);
        lblSuccessMessage.setManaged(false);
    }
}
