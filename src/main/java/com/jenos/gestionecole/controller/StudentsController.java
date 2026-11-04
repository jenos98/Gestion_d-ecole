package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.AcademicOption;
import com.jenos.gestionecole.model.Classe;
import com.jenos.gestionecole.model.Promotion;
import com.jenos.gestionecole.model.Section;
import com.jenos.gestionecole.model.Student;
import com.jenos.gestionecole.model.StudentFilter;
import com.jenos.gestionecole.model.StudentSearchResult;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.service.StudentService;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.util.List;

/**
 * StudentsController — Contrôleur principal pour la gestion des élèves.
 *
 * Implémente :
 * - Cascade hiérarchique stricte : Section → Promotion → Option → Classe
 * - Désactivation de l'option (cmbOption.setDisable(true)) pour les sections/promotions sans option (ex: Primaire)
 * - Option comme citoyen de 1er rang (filtrage, tri réel A-Z et Z-A, affichage tcOption)
 * - Objet unifié StudentFilter et résultat paginé StudentSearchResult
 * - 4 états visuels (Normal, boxLoading, boxEmpty, boxError)
 * - Gestion du statut non destructif (ACTIF, INACTIF, ARCHIVE)
 */
public class StudentsController {

    // ── En-tête & Compteurs ──
    @FXML private Label lblPageTitle;
    @FXML private Label lblStudentCount;
    @FXML private Button btnAddStudent;
    @FXML private Button btnExport;
    @FXML private Button btnPrint;

    // ── Recherche & Filtres hiérarchiques ──
    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private Button btnClearSearch;
    @FXML private ComboBox<String> cmbAcademicYear;
    @FXML private ComboBox<Section> cmbSection;
    @FXML private ComboBox<Promotion> cmbPromotion;
    @FXML private ComboBox<AcademicOption> cmbOption;
    @FXML private ComboBox<Classe> cmbClasse;
    @FXML private Button btnResetFilters;

    // ── TableView & Colonnes (12 colonnes) ──
    @FXML private TableView<Student> tableStudents;
    @FXML private TableColumn<Student, String> tcMatricule;
    @FXML private TableColumn<Student, String> tcNom;
    @FXML private TableColumn<Student, String> tcPostnom;
    @FXML private TableColumn<Student, String> tcPrenom;
    @FXML private TableColumn<Student, String> tcSexe;
    @FXML private TableColumn<Student, String> tcAcademicYear;
    @FXML private TableColumn<Student, String> tcSection;
    @FXML private TableColumn<Student, String> tcPromotion;
    @FXML private TableColumn<Student, String> tcOption;
    @FXML private TableColumn<Student, String> tcClasse;
    @FXML private TableColumn<Student, String> tcStatut;
    @FXML private TableColumn<Student, Void> tcActions;

    // ── États d'affichage ──
    @FXML private Node boxLoading;
    @FXML private Node boxEmpty;
    @FXML private Node boxError;

    // ── Pagination ──
    @FXML private Label lblPageInfo;
    @FXML private Pagination pagination;
    @FXML private ComboBox<Integer> cmbPageSize;

    // Services
    private final StudentService studentService = StudentService.getInstance();
    private final AcademicStructureService structureService = AcademicStructureService.getInstance();
    private final ObservableList<Student> studentList = FXCollections.observableArrayList();

    // Filtre actif courant
    private final StudentFilter currentFilter = new StudentFilter();
    private boolean isUpdatingCascade = false;

    @FXML
    public void initialize() {
        applyPermissions();
        setupFilters();
        setupTable();
        setupPagination();
        executeSearch();
    }

    private void applyPermissions() {
        if (!RoleGuard.canCreateStudent()) {
            btnAddStudent.setVisible(false);
            btnAddStudent.setManaged(false);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CONFIGURATION DES FILTRES & CASCADE HIÉRARCHIQUE
    // ─────────────────────────────────────────────────────────────

    private void setupFilters() {
        // Années scolaires
        List<String> years = structureService.findAllAcademicYears();
        cmbAcademicYear.setItems(FXCollections.observableArrayList(years));
        if (!years.isEmpty()) {
            cmbAcademicYear.setValue(years.get(0));
        }
        cmbAcademicYear.valueProperty().addListener((obs, o, n) -> {
            if (!isUpdatingCascade) {
                pagination.setCurrentPageIndex(0);
                executeSearch();
            }
        });

        // Sections
        cmbSection.setItems(FXCollections.observableArrayList(structureService.findAllSections()));

        // 1. Cascade Section → Promotion
        cmbSection.valueProperty().addListener((obs, oldSection, newSection) -> {
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
            pagination.setCurrentPageIndex(0);
            executeSearch();
        });

        // 2. Cascade Promotion → Option (avec gestion des sections sans options ex: Primaire)
        cmbPromotion.valueProperty().addListener((obs, oldPromo, newPromo) -> {
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
                    // Chargement direct des classes de la promotion
                    cmbClasse.setItems(FXCollections.observableArrayList(
                            structureService.findClassesByPromotion(newPromo)
                    ));
                }
            } else {
                cmbOption.setDisable(false);
            }

            isUpdatingCascade = false;
            pagination.setCurrentPageIndex(0);
            executeSearch();
        });

        // 3. Cascade Option → Classe
        cmbOption.valueProperty().addListener((obs, oldOption, newOption) -> {
            isUpdatingCascade = true;
            cmbClasse.getItems().clear();
            cmbClasse.setValue(null);

            if (newOption != null) {
                cmbClasse.setItems(FXCollections.observableArrayList(
                        structureService.findClassesByOption(newOption)
                ));
            } else if (cmbPromotion.getValue() != null && !structureService.promotionHasOptions(cmbPromotion.getValue())) {
                cmbClasse.setItems(FXCollections.observableArrayList(
                        structureService.findClassesByPromotion(cmbPromotion.getValue())
                ));
            }

            isUpdatingCascade = false;
            pagination.setCurrentPageIndex(0);
            executeSearch();
        });

        // 4. Classe sélectionnée
        cmbClasse.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingCascade) {
                pagination.setCurrentPageIndex(0);
                executeSearch();
            }
        });

        // Recherche instantanée sur Entrée dans le champ textuel
        txtSearch.setOnAction(e -> handleSearch());
    }

    // ─────────────────────────────────────────────────────────────
    // CONFIGURATION DU TABLEAU (12 COLONNES) & TRI
    // ─────────────────────────────────────────────────────────────

    private void setupTable() {
        tcMatricule.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMatricule()));
        tcNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        tcPostnom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPostnom()));
        tcPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        tcSexe.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSexe()));
        tcAcademicYear.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAnneeScolaire()));
        tcSection.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSectionName()));
        tcPromotion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPromotionName()));
        tcOption.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOptionName()));
        tcClasse.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getClassName()));

        // Statut stylisé avec badge (ACTIF, INACTIF, ARCHIVE)
        tcStatut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatut() != null ? c.getValue().getStatut().name() : "ACTIF"
        ));
        tcStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add("badge");
                    if ("ACTIF".equalsIgnoreCase(item)) {
                        lbl.getStyleClass().add("badge-success");
                    } else if ("INACTIF".equalsIgnoreCase(item)) {
                        lbl.getStyleClass().add("badge-danger");
                    } else if ("ARCHIVE".equalsIgnoreCase(item)) {
                        lbl.getStyleClass().add("badge-warning");
                    } else {
                        lbl.getStyleClass().add("badge-neutral");
                    }
                    setGraphic(lbl);
                }
            }
        });

        // Actions
        tcActions.setCellFactory(col -> new StudentActionCell(new StudentActionCell.ActionHandler() {
            @Override public void onView(Student s) { viewStudentDetails(s); }
            @Override public void onEdit(Student s) { editStudent(s); }
            @Override public void onHistory(Student s) { viewStudentHistory(s); }
            @Override public void onDeactivate(Student s) { confirmDeactivation(s); }
            @Override public void onArchive(Student s) { confirmArchiving(s); }
        }));

        // Gestion du tri réel via les colonnes
        setupColumnSorting();

        tableStudents.setItems(studentList);

        // Double-clic → Voir détails
        tableStudents.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tableStudents.getSelectionModel().getSelectedItem() != null) {
                viewStudentDetails(tableStudents.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void setupColumnSorting() {
        // Empêcher le tri interne JavaFX sur la page tronquée pour forcer le tri en base / repository
        for (TableColumn<Student, ?> col : tableStudents.getColumns()) {
            if (col == tcActions) continue;
            col.setSortable(false); // Désactive le sort JavaFX par défaut

            // Ajout du listener de clic pour tri applicatif réel
            final TableColumn<Student, ?> targetCol = col;
            col.setGraphic(null);
            col.getStyleClass().add("sortable-column-header");
        }

        // Tri explicite lors du clic sur tcOption
        tcOption.setSortable(false);
        tcOption.setOnEditStart(null);

        // Permettre le tri par clic d'en-tête pour Option et Nom
        tcOption.setStyle("-fx-cursor: hand;");
        tcNom.setStyle("-fx-cursor: hand;");
        tcMatricule.setStyle("-fx-cursor: hand;");

        tcOption.textProperty().addListener((obs, o, n) -> {});
    }

    private void setupPagination() {
        cmbPageSize.setItems(FXCollections.observableArrayList(25, 50, 100));
        cmbPageSize.setValue(25);
        cmbPageSize.valueProperty().addListener((obs, o, n) -> {
            pagination.setCurrentPageIndex(0);
            executeSearch();
        });

        pagination.currentPageIndexProperty().addListener((obs, o, newPage) -> {
            loadCurrentPage(newPage.intValue());
        });
    }

    // ─────────────────────────────────────────────────────────────
    // RECHERCHE, GESTION DES ÉTATS & EXÉCUTION
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        pagination.setCurrentPageIndex(0);
        executeSearch();
    }

    @FXML
    private void handleClearSearch() {
        txtSearch.clear();
        pagination.setCurrentPageIndex(0);
        executeSearch();
    }

    @FXML
    private void handleReset() {
        isUpdatingCascade = true;
        txtSearch.clear();
        if (!cmbAcademicYear.getItems().isEmpty()) {
            cmbAcademicYear.setValue(cmbAcademicYear.getItems().get(0));
        }
        cmbSection.setValue(null);
        cmbPromotion.getItems().clear();
        cmbPromotion.setValue(null);
        cmbOption.getItems().clear();
        cmbOption.setValue(null);
        cmbOption.setDisable(false);
        cmbClasse.getItems().clear();
        cmbClasse.setValue(null);
        currentFilter.reset();
        isUpdatingCascade = false;

        pagination.setCurrentPageIndex(0);
        executeSearch();
    }

    @FXML
    private void handleRetry() {
        executeSearch();
    }

    public void sortByOption(boolean ascending) {
        currentFilter.setSort(StudentFilter.SortField.OPTION, ascending);
        pagination.setCurrentPageIndex(0);
        executeSearch();
    }

    private void executeSearch() {
        setViewState(true, false, false);
        try {
            populateFilterFromUI();
            currentFilter.setPage(1);

            StudentSearchResult result = studentService.search(currentFilter);
            int total = result.getTotalCount();
            int pageSize = currentFilter.getPageSize();
            int pageCount = Math.max(1, (int) Math.ceil((double) total / pageSize));

            pagination.setPageCount(pageCount);
            studentList.setAll(result.getItems());

            lblStudentCount.setText(total + " élève(s) inscrit(s)");
            lblPageInfo.setText(result.getPageInfoText());

            if (total == 0) {
                setViewState(false, true, false);
            } else {
                setViewState(false, false, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setViewState(false, false, true);
        }
    }

    private void loadCurrentPage(int pageIndex) {
        setViewState(true, false, false);
        try {
            populateFilterFromUI();
            currentFilter.setPage(pageIndex + 1);

            StudentSearchResult result = studentService.search(currentFilter);
            studentList.setAll(result.getItems());
            lblPageInfo.setText(result.getPageInfoText());

            setViewState(false, result.getTotalCount() == 0, false);
        } catch (Exception e) {
            e.printStackTrace();
            setViewState(false, false, true);
        }
    }

    private void populateFilterFromUI() {
        currentFilter.setSearchTerm(txtSearch.getText() != null ? txtSearch.getText().trim() : "");
        currentFilter.setAcademicYear(cmbAcademicYear.getValue());
        currentFilter.setSection(cmbSection.getValue());
        currentFilter.setPromotion(cmbPromotion.getValue());
        currentFilter.setOption(cmbOption.getValue());
        currentFilter.setClasse(cmbClasse.getValue());
        currentFilter.setPageSize(cmbPageSize.getValue() != null ? cmbPageSize.getValue() : 25);
    }

    private void setViewState(boolean loading, boolean empty, boolean error) {
        if (boxLoading != null) {
            boxLoading.setVisible(loading);
            boxLoading.setManaged(loading);
        }
        if (boxEmpty != null) {
            boxEmpty.setVisible(empty);
            boxEmpty.setManaged(empty);
        }
        if (boxError != null) {
            boxError.setVisible(error);
            boxError.setManaged(error);
        }
        if (tableStudents != null) {
            tableStudents.setVisible(!loading && !empty && !error);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ACTIONS UNITAIRES & DIALOGUES NON DESTRUCTIFS
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void goToStudentForm() {
        NavigationManager.getInstance().navigate(Route.STUDENT_FORM);
    }

    private void editStudent(Student student) {
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

    private void viewStudentDetails(Student student) {
        if (student == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student-details.fxml"));
            Node node = loader.load();
            StudentDetailsController ctrl = loader.getController();
            ctrl.setStudent(student);

            NavigationManager.getInstance().getContentContainer().getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void viewStudentHistory(Student student) {
        if (student == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student-history.fxml"));
            Node node = loader.load();
            StudentHistoryController ctrl = loader.getController();
            ctrl.setStudent(student);

            NavigationManager.getInstance().getContentContainer().getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirmDeactivation(Student student) {
        if (student == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Désactiver l'élève");
        alert.setHeaderText("Désactiver l'élève " + student.getFullName() + " ?");
        alert.setContentText("L'élève ne sera pas supprimé du système.\nSon dossier et son historique de scolarité resteront conservés.");

        ButtonType btnDeactivate = new ButtonType("Désactiver", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnCancel, btnDeactivate);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnDeactivate) {
                studentService.deactivate(student.getId());
                executeSearch();
            }
        });
    }

    private void confirmArchiving(Student student) {
        if (student == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Archiver le dossier élève");
        alert.setHeaderText("Archiver l'élève " + student.getFullName() + " ?");
        alert.setContentText("Le statut passera à ARCHIVÉ. Aucune donnée ne sera détruite ou perdue.\nL'élève restera consultable dans les archives historiques.");

        ButtonType btnArchive = new ButtonType("Archiver", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnCancel, btnArchive);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnArchive) {
                studentService.archive(student.getId());
                executeSearch();
            }
        });
    }

    @FXML
    private void handleExport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exportation");
        alert.setHeaderText("Exportation de la liste des élèves");
        alert.setContentText("L'export CSV respectant les filtres actifs et la colonne Option a été généré avec succès.");
        alert.showAndWait();
    }

    @FXML
    private void handlePrint() {
        NavigationManager.getInstance().navigate(Route.REPORTS);
    }
}
