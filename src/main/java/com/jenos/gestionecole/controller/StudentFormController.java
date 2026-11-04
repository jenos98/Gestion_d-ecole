package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.AcademicOption;
import com.jenos.gestionecole.model.Classe;
import com.jenos.gestionecole.model.Promotion;
import com.jenos.gestionecole.model.Section;
import com.jenos.gestionecole.model.Student;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.service.StudentService;
import com.jenos.gestionecole.service.StudentValidator;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;

/**
 * StudentFormController — Formulaire de création / modification d'un élève.
 *
 * Implémente :
 * - Règle hiérarchique absolue : Section → Promotion → Option → Classe
 * - Désactivation réactive de l'option pour les sections/promotions sans option (ex: Primaire)
 * - Section Tuteur / Responsable légal complète
 * - Verrouillage du matricule en mode modification (non éditable)
 * - Contrôle dirty à l'annulation et confirmation modale à l'enregistrement
 */
public class StudentFormController {

    // ── En-tête formulaire ──
    @FXML private Label lblFormTitle;
    @FXML private Label lblFormSubtitle;

    // ── 1. Identité ──
    @FXML private TextField txtNom;
    @FXML private TextField txtPostnom;
    @FXML private TextField txtPrenom;
    @FXML private ComboBox<String> cmbSexe;
    @FXML private DatePicker dpDateNaissance;
    @FXML private TextField txtLieuNaissance;
    @FXML private TextField txtNationalite;

    // ── 2. Responsable / Tuteur & Contact ──
    @FXML private TextField txtGuardianName;
    @FXML private TextField txtGuardianPhone;
    @FXML private ComboBox<String> cmbGuardianRelation;
    @FXML private TextField txtGuardianAddress;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtEmail;

    // ── 3. Scolarité (Cascade Section › Promotion › Option › Classe) ──
    @FXML private ComboBox<String> cmbAnneeScolaire;
    @FXML private ComboBox<Section> cmbSection;
    @FXML private ComboBox<Promotion> cmbPromotion;
    @FXML private ComboBox<AcademicOption> cmbOption;
    @FXML private ComboBox<Classe> cmbClasse;
    @FXML private TextField txtMatricule;
    @FXML private Button btnGenerateMatricule;

    // ── Erreurs & Actions ──
    @FXML private Label lblValidationError;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final AcademicStructureService structureService = AcademicStructureService.getInstance();
    private final StudentService studentService = StudentService.getInstance();
    private Student currentStudent; // null = création, non-null = modification
    private boolean isUpdatingCascade = false;

    @FXML
    public void initialize() {
        setupFormFields();
        setupHierarchicalCascade();
        resetToCreationMode();
    }

    private void setupFormFields() {
        cmbSexe.setItems(FXCollections.observableArrayList("M", "F"));
        txtNationalite.setText("Congolaise");

        cmbGuardianRelation.setItems(FXCollections.observableArrayList(
                "Père", "Mère", "Tuteur légal", "Oncle", "Tante", "Grand-parent", "Autre"
        ));

        cmbAnneeScolaire.setItems(
                FXCollections.observableArrayList(structureService.findAllAcademicYears())
        );
        if (!cmbAnneeScolaire.getItems().isEmpty()) {
            cmbAnneeScolaire.setValue(cmbAnneeScolaire.getItems().get(0));
        }

        cmbSection.setItems(
                FXCollections.observableArrayList(structureService.findAllSections())
        );

        lblValidationError.setVisible(false);
        lblValidationError.setManaged(false);
    }

    // ─────────────────────────────────────────────────────────────
    // CASCADE HIÉRARCHIQUE STRICTE
    // ─────────────────────────────────────────────────────────────

    private void setupHierarchicalCascade() {
        // 1. Section → Promotion
        cmbSection.valueProperty().addListener((obs, oldSection, newSection) -> {
            if (isUpdatingCascade) return;
            isUpdatingCascade = true;

            cmbPromotion.getItems().clear();
            cmbPromotion.setValue(null);
            cmbOption.getItems().clear();
            cmbOption.setValue(null);
            cmbOption.setDisable(false);
            cmbClasse.getItems().clear();
            cmbClasse.setValue(null);

            if (newSection != null) {
                cmbPromotion.setItems(FXCollections.observableArrayList(
                        structureService.findPromotionsBySection(newSection)
                ));
            }
            isUpdatingCascade = false;
        });

        // 2. Promotion → Option (Désactivée pour les sections sans options comme le Primaire)
        cmbPromotion.valueProperty().addListener((obs, oldPromo, newPromo) -> {
            if (isUpdatingCascade) return;
            isUpdatingCascade = true;

            cmbOption.getItems().clear();
            cmbOption.setValue(null);
            cmbClasse.getItems().clear();
            cmbClasse.setValue(null);

            if (newPromo != null) {
                boolean hasOptions = structureService.promotionHasOptions(newPromo);
                if (hasOptions) {
                    cmbOption.setDisable(false);
                    cmbOption.setItems(FXCollections.observableArrayList(
                            structureService.findOptionsByPromotion(newPromo)
                    ));
                } else {
                    // Promotion sans option (ex: Primaire) : cmbOption désactivé
                    cmbOption.setDisable(true);
                    cmbClasse.setItems(FXCollections.observableArrayList(
                            structureService.findClassesByPromotion(newPromo)
                    ));
                }
            } else {
                cmbOption.setDisable(false);
            }
            isUpdatingCascade = false;
        });

        // 3. Option → Classe
        cmbOption.valueProperty().addListener((obs, oldOpt, newOpt) -> {
            if (isUpdatingCascade) return;
            isUpdatingCascade = true;

            cmbClasse.getItems().clear();
            cmbClasse.setValue(null);

            if (newOpt != null) {
                cmbClasse.setItems(FXCollections.observableArrayList(
                        structureService.findClassesByOption(newOpt)
                ));
            } else if (cmbPromotion.getValue() != null && !structureService.promotionHasOptions(cmbPromotion.getValue())) {
                cmbClasse.setItems(FXCollections.observableArrayList(
                        structureService.findClassesByPromotion(cmbPromotion.getValue())
                ));
            }
            isUpdatingCascade = false;
        });
    }

    @FXML
    private void generateMatricule() {
        String annee = cmbAnneeScolaire.getValue();
        Section section = cmbSection.getValue();
        String generated = studentService.generateMatricule(annee, section);
        txtMatricule.setText(generated);
    }

    // ─────────────────────────────────────────────────────────────
    // MODES CRÉATION / MODIFICATION
    // ─────────────────────────────────────────────────────────────

    public void setStudent(Student student) {
        this.currentStudent = student;
        if (student == null) {
            resetToCreationMode();
            return;
        }

        lblFormTitle.setText("Modification de l'Élève : " + student.getFullName());
        lblFormSubtitle.setText("Matricule : " + student.getMatricule() + " — Statut : " + student.getStatut());
        btnSave.setText("Enregistrer les modifications");

        // En mode modification : le matricule est figé et non éditable
        txtMatricule.setEditable(false);
        txtMatricule.getStyleClass().add("input-readonly");
        btnGenerateMatricule.setDisable(true);

        // 1. Identité
        txtNom.setText(student.getNom());
        txtPostnom.setText(student.getPostnom());
        txtPrenom.setText(student.getPrenom());
        cmbSexe.setValue(student.getSexe());
        dpDateNaissance.setValue(student.getDateNaissance());
        txtLieuNaissance.setText(student.getLieuNaissance());
        txtNationalite.setText(student.getNationalite() != null ? student.getNationalite() : "Congolaise");

        // 2. Responsable / Tuteur & Contact
        txtGuardianName.setText(student.getGuardianName());
        txtGuardianPhone.setText(student.getGuardianPhone());
        cmbGuardianRelation.setValue(student.getGuardianRelation());
        txtGuardianAddress.setText(student.getGuardianAddress() != null ? student.getGuardianAddress() : student.getAdresse());
        txtTelephone.setText(student.getTelephone());
        txtEmail.setText(student.getEmail());

        // 3. Scolarité (Chargement séquentiel avec bypass listener)
        isUpdatingCascade = true;
        txtMatricule.setText(student.getMatricule());
        cmbAnneeScolaire.setValue(student.getAnneeScolaire());

        if (student.getSection() != null) {
            cmbSection.setValue(student.getSection());
            cmbPromotion.setItems(FXCollections.observableArrayList(
                    structureService.findPromotionsBySection(student.getSection())
            ));
        }

        if (student.getPromotion() != null) {
            cmbPromotion.setValue(student.getPromotion());
            boolean hasOptions = structureService.promotionHasOptions(student.getPromotion());
            if (hasOptions) {
                cmbOption.setDisable(false);
                cmbOption.setItems(FXCollections.observableArrayList(
                        structureService.findOptionsByPromotion(student.getPromotion())
                ));
                if (student.getOption() != null) {
                    cmbOption.setValue(student.getOption());
                    cmbClasse.setItems(FXCollections.observableArrayList(
                            structureService.findClassesByOption(student.getOption())
                    ));
                }
            } else {
                cmbOption.setDisable(true);
                cmbOption.setValue(null);
                cmbClasse.setItems(FXCollections.observableArrayList(
                        structureService.findClassesByPromotion(student.getPromotion())
                ));
            }
        }

        if (student.getClasse() != null) {
            cmbClasse.setValue(student.getClasse());
        }
        isUpdatingCascade = false;
    }

    private void resetToCreationMode() {
        this.currentStudent = null;
        lblFormTitle.setText("Inscription / Nouveau Dossier Élève");
        lblFormSubtitle.setText("Renseignez les informations personnelles, le tuteur légal et le cursus scolaire de l'élève.");
        btnSave.setText("Enregistrer l'élève");

        txtMatricule.setEditable(true);
        txtMatricule.getStyleClass().remove("input-readonly");
        btnGenerateMatricule.setDisable(false);

        txtNom.clear();
        txtPostnom.clear();
        txtPrenom.clear();
        cmbSexe.setValue(null);
        dpDateNaissance.setValue(null);
        txtLieuNaissance.clear();
        txtNationalite.setText("Congolaise");

        txtGuardianName.clear();
        txtGuardianPhone.clear();
        cmbGuardianRelation.setValue(null);
        txtGuardianAddress.clear();
        txtTelephone.clear();
        txtEmail.clear();
        txtMatricule.clear();

        isUpdatingCascade = true;
        cmbSection.setValue(null);
        cmbPromotion.getItems().clear();
        cmbOption.getItems().clear();
        cmbOption.setDisable(false);
        cmbClasse.getItems().clear();
        isUpdatingCascade = false;
    }

    // ─────────────────────────────────────────────────────────────
    // SAUVEGARDE & ANNULATION AVEC CONTRÔLES
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        clearError();
        Student student = buildStudentFromForm();

        // 1. Validation métier stricte
        StudentValidator.ValidationResult result = StudentValidator.validate(student, studentService);
        if (!result.isValid()) {
            showError(result.getFirstError());
            return;
        }

        if (btnSave != null) btnSave.setDisable(true);
        try {
            // 2. Dialogue de confirmation obligatoire avant enregistrement
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation d'enregistrement");
            confirmAlert.setHeaderText("Confirmer l'enregistrement du dossier ?");
            confirmAlert.setContentText("Les informations de l'élève " + student.getFullName() + " seront enregistrées dans le système.");

            ButtonType btnConfirm = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmAlert.getButtonTypes().setAll(btnAnnuler, btnConfirm);

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == btnConfirm) {
                    studentService.save(student);
                    NavigationManager.getInstance().navigate(Route.STUDENTS);
                }
            });
        } finally {
            if (btnSave != null) btnSave.setDisable(false);
        }
    }

    private Student buildStudentFromForm() {
        Student s = currentStudent != null ? currentStudent : new Student();
        s.setNom(txtNom.getText() != null ? txtNom.getText().trim() : "");
        s.setPostnom(txtPostnom.getText() != null ? txtPostnom.getText().trim() : "");
        s.setPrenom(txtPrenom.getText() != null ? txtPrenom.getText().trim() : "");
        s.setSexe(cmbSexe.getValue());
        s.setDateNaissance(dpDateNaissance.getValue());
        s.setLieuNaissance(txtLieuNaissance.getText() != null ? txtLieuNaissance.getText().trim() : "");
        s.setNationalite(txtNationalite.getText() != null ? txtNationalite.getText().trim() : "Congolaise");

        s.setGuardianName(txtGuardianName.getText() != null ? txtGuardianName.getText().trim() : "");
        s.setGuardianPhone(txtGuardianPhone.getText() != null ? txtGuardianPhone.getText().trim() : "");
        s.setGuardianRelation(cmbGuardianRelation.getValue());
        s.setGuardianAddress(txtGuardianAddress.getText() != null ? txtGuardianAddress.getText().trim() : "");

        s.setTelephone(txtTelephone.getText() != null ? txtTelephone.getText().trim() : "");
        s.setEmail(txtEmail.getText() != null ? txtEmail.getText().trim() : "");
        s.setAdresse(txtGuardianAddress.getText() != null ? txtGuardianAddress.getText().trim() : "");

        s.setMatricule(txtMatricule.getText() != null ? txtMatricule.getText().trim() : "");
        s.setAnneeScolaire(cmbAnneeScolaire.getValue());

        s.setSection(cmbSection.getValue());
        s.setPromotion(cmbPromotion.getValue());
        s.setOption(cmbOption.getValue());
        s.setClasse(cmbClasse.getValue());

        return s;
    }

    @FXML
    private void handleCancel() {
        if (isFormDirty()) {
            Alert confirmExit = new Alert(Alert.AlertType.CONFIRMATION);
            confirmExit.setTitle("Abandonner les modifications");
            confirmExit.setHeaderText("Des modifications n'ont pas été enregistrées.");
            confirmExit.setContentText("Êtes-vous sûr de vouloir quitter cette page ? Vos saisies seront perdues.");

            ButtonType btnYes = new ButtonType("Quitter", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnNo = new ButtonType("Rester", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmExit.getButtonTypes().setAll(btnNo, btnYes);

            confirmExit.showAndWait().ifPresent(response -> {
                if (response == btnYes) {
                    NavigationManager.getInstance().navigate(Route.STUDENTS);
                }
            });
        } else {
            NavigationManager.getInstance().navigate(Route.STUDENTS);
        }
    }

    private boolean isFormDirty() {
        if (currentStudent == null) {
            return (txtNom.getText() != null && !txtNom.getText().isBlank())
                    || (txtPostnom.getText() != null && !txtPostnom.getText().isBlank())
                    || (txtPrenom.getText() != null && !txtPrenom.getText().isBlank())
                    || (txtMatricule.getText() != null && !txtMatricule.getText().isBlank());
        }
        // En modification, vérifier si des valeurs ont changé
        return !matchesValue(txtNom.getText(), currentStudent.getNom())
                || !matchesValue(txtPostnom.getText(), currentStudent.getPostnom())
                || !matchesValue(txtPrenom.getText(), currentStudent.getPrenom())
                || !matchesValue(txtTelephone.getText(), currentStudent.getTelephone())
                || !matchesValue(txtGuardianName.getText(), currentStudent.getGuardianName());
    }

    private boolean matchesValue(String s1, String s2) {
        String safe1 = s1 != null ? s1.trim() : "";
        String safe2 = s2 != null ? s2.trim() : "";
        return safe1.equalsIgnoreCase(safe2);
    }

    private void showError(String msg) {
        lblValidationError.setText(msg);
        lblValidationError.setVisible(true);
        lblValidationError.setManaged(true);
    }

    private void clearError() {
        lblValidationError.setVisible(false);
        lblValidationError.setManaged(false);
    }
}
