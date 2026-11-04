package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Staff;
import com.jenos.gestionecole.service.StaffService;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * StaffController — Gestion et affichage du personnel.
 *
 * RÈGLES :
 * - Pas de colonne salaire dans la vue liste
 * - Archivage non destructif (statut ARCHIVE)
 * - RoleGuard contrôle l'accès aux actions
 */
public class StaffController {

    // Header
    @FXML private Label lblPageTitle;
    @FXML private Label lblStaffCount;
    @FXML private Button btnAddStaff;

    // Recherche
    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private Button btnClearSearch;

    // Filtres
    @FXML private ComboBox<String> cmbDepartment;
    @FXML private ComboBox<String> cmbFunction;
    @FXML private ComboBox<Staff.StaffStatus> cmbStatus;
    @FXML private Button btnResetFilters;

    // Table
    @FXML private StackPane tableContainer;
    @FXML private TableView<Staff> tblStaff;
    @FXML private TableColumn<Staff, String> tcMatricule;
    @FXML private TableColumn<Staff, String> tcNom;
    @FXML private TableColumn<Staff, String> tcPostnom;
    @FXML private TableColumn<Staff, String> tcPrenom;
    @FXML private TableColumn<Staff, String> tcFonction;
    @FXML private TableColumn<Staff, String> tcDepartment;
    @FXML private TableColumn<Staff, String> tcPhone;
    @FXML private TableColumn<Staff, String> tcStatus;
    @FXML private TableColumn<Staff, Void> tcActions;

    // États UI
    @FXML private VBox boxLoading;
    @FXML private VBox boxEmpty;
    @FXML private VBox boxError;

    // Pagination
    @FXML private Label lblPageInfo;
    @FXML private Pagination pgStaff;

    private final StaffService staffService = StaffService.getInstance();
    private final ObservableList<Staff> staffList = FXCollections.observableArrayList();

    private static final int PAGE_SIZE = 25;

    @FXML
    public void initialize() {
        setupPermissions();
        setupFilters();
        setupTable();
        loadStaff();
    }

    // ─── Permissions ───
    private void setupPermissions() {
        if (btnAddStaff != null) {
            btnAddStaff.setVisible(RoleGuard.canCreateStaff());
            btnAddStaff.setManaged(RoleGuard.canCreateStaff());
        }
    }

    // ─── Filtres ───
    private void setupFilters() {
        // Départements (catégories)
        cmbDepartment.setItems(FXCollections.observableArrayList(staffService.findAllDepartments()));

        // Fonctions
        cmbFunction.setItems(FXCollections.observableArrayList(staffService.findAllFonctions()));

        // Statuts
        ObservableList<Staff.StaffStatus> statuses = FXCollections.observableArrayList();
        for (Staff.StaffStatus s : Staff.StaffStatus.values()) {
            // N'afficher ARCHIVE que si l'utilisateur a la permission
            if (s == Staff.StaffStatus.ARCHIVE && !RoleGuard.canArchiveStaff()) continue;
            statuses.add(s);
        }
        cmbStatus.setItems(statuses);

        // Listeners
        cmbDepartment.valueProperty().addListener((obs, o, n) -> search());
        cmbFunction.valueProperty().addListener((obs, o, n) -> search());
        cmbStatus.valueProperty().addListener((obs, o, n) -> search());
    }

    // ─── Table ───
    private void setupTable() {
        tcMatricule.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMatricule()));
        tcNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom() != null ? c.getValue().getNom().toUpperCase() : ""));
        tcPostnom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPostnom()));
        tcPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        tcFonction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFonction()));
        tcDepartment.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartment()));
        tcPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelephone()));
        tcStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatut() != null ? c.getValue().getStatut().getLabel() : ""));

        // Colonne Actions : Voir / Modifier / Archiver
        tcActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("👁");
            private final Button btnEdit = new Button("✏");
            private final Button btnArchive = new Button("📦");
            private final HBox container;

            {
                btnView.getStyleClass().add("btn-cell-action");
                btnEdit.getStyleClass().add("btn-cell-action");
                btnArchive.getStyleClass().add("btn-cell-action");

                btnView.setTooltip(new Tooltip("Voir"));
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnArchive.setTooltip(new Tooltip("Archiver"));

                // Archiver visible uniquement si autorisé
                btnArchive.setVisible(RoleGuard.canArchiveStaff());
                btnArchive.setManaged(RoleGuard.canArchiveStaff());

                if (RoleGuard.canArchiveStaff()) {
                    container = new HBox(6, btnView, btnEdit, btnArchive);
                } else {
                    container = new HBox(6, btnView, btnEdit);
                }
                container.setAlignment(Pos.CENTER);

                btnView.setOnAction(e -> {
                    Staff s = getTableView().getItems().get(getIndex());
                    if (s != null) openDetails(s);
                });

                btnEdit.setOnAction(e -> {
                    Staff s = getTableView().getItems().get(getIndex());
                    if (s != null) openForm(s);
                });

                btnArchive.setOnAction(e -> {
                    Staff s = getTableView().getItems().get(getIndex());
                    if (s != null) archiveStaff(s);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Staff s = getTableView().getItems().get(getIndex());
                    // Masquer archivage si déjà archivé
                    if (s != null && s.getStatut() == Staff.StaffStatus.ARCHIVE) {
                        btnArchive.setDisable(true);
                    } else {
                        btnArchive.setDisable(false);
                    }
                    setGraphic(container);
                }
            }
        });

        tblStaff.setItems(staffList);
        tblStaff.setPlaceholder(new Label("Aucun membre du personnel trouvé."));
    }

    // ─── Recherche ───
    @FXML
    private void handleSearch() {
        search();
    }

    @FXML
    private void handleClearSearch() {
        txtSearch.clear();
        search();
    }

    @FXML
    private void handleReset() {
        txtSearch.clear();
        cmbDepartment.setValue(null);
        cmbFunction.setValue(null);
        cmbStatus.setValue(null);
        search();
    }

    private void search() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().trim() : "";
        String dept = cmbDepartment.getValue();
        if ("Tous".equalsIgnoreCase(dept)) dept = null;
        String fn = cmbFunction.getValue();
        if ("Toutes".equalsIgnoreCase(fn)) fn = null;
        Staff.StaffStatus st = cmbStatus.getValue();

        List<Staff> results = staffService.search(keyword, dept, fn, st);
        staffList.setAll(results);

        updateUI(results);
    }

    private void updateUI(List<Staff> results) {
        // Compteur
        int activeCount = (int) results.stream()
                .filter(s -> s.getStatut() != Staff.StaffStatus.ARCHIVE)
                .count();
        lblStaffCount.setText(activeCount + " membre(s) actif(s) / " + results.size() + " total");

        // États UI
        boolean hasData = !results.isEmpty();
        tblStaff.setVisible(hasData);
        tblStaff.setManaged(hasData);
        if (boxEmpty != null) {
            boxEmpty.setVisible(!hasData);
            boxEmpty.setManaged(!hasData);
        }

        // Page info
        if (lblPageInfo != null) {
            if (results.isEmpty()) {
                lblPageInfo.setText("Affichage 0 à 0 sur 0");
            } else {
                lblPageInfo.setText("Affichage 1 à " + results.size() + " sur " + results.size());
            }
        }
    }

    // ─── Actions ───
    @FXML
    private void handleNewStaff() {
        openForm(null);
    }

    private void openDetails(Staff staff) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/staff-details.fxml"));
            javafx.scene.Node node = loader.load();
            StaffDetailsController ctrl = loader.getController();
            ctrl.setStaff(staff);
            NavigationManager.getInstance().getContentContainer().getChildren().setAll(node);
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    private void openForm(Staff staff) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/staff-form.fxml"));
            javafx.scene.Node node = loader.load();
            StaffFormController ctrl = loader.getController();
            ctrl.setStaff(staff);
            NavigationManager.getInstance().getContentContainer().getChildren().setAll(node);
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }

    private void archiveStaff(Staff staff) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Archiver un membre");
        alert.setHeaderText("Archiver " + staff.getFullName() + " ?");
        alert.setContentText("Le membre sera archivé (non supprimé). Cette action est réversible par un administrateur.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = staffService.archive(staff.getId());
                if (success) {
                    search(); // Rafraîchir la liste
                }
            }
        });
    }

    private void loadStaff() {
        search();
    }
}
