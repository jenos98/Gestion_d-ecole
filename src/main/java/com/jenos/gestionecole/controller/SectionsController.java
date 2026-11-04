package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Section;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.util.RoleGuard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

/**
 * SectionsController — Contrôleur de gestion des sections (sections.fxml).
 */
public class SectionsController {

    @FXML private Label lblFormTitle;
    @FXML private Label lblMessage;
    @FXML private TextField txtCode;
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private CheckBox chkActive;
    @FXML private Button btnReset;
    @FXML private Button btnSave;

    @FXML private TableView<Section> tableSections;
    @FXML private TableColumn<Section, String> tcCode;
    @FXML private TableColumn<Section, String> tcName;
    @FXML private TableColumn<Section, String> tcDescription;
    @FXML private TableColumn<Section, String> tcStatus;
    @FXML private TableColumn<Section, Void> tcActions;

    private final AcademicStructureService academicService = AcademicStructureService.getInstance();
    private final ObservableList<Section> sectionList = FXCollections.observableArrayList();
    private Section currentEditingSection = null;

    @FXML
    public void initialize() {
        setupTable();
        loadSections();
    }

    private void setupTable() {
        tcCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        tcName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tcDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        tcStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Active" : "Inactive"));

        tcStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("Active".equalsIgnoreCase(item)
                            ? "-fx-text-fill: #198754; -fx-font-weight: bold;"
                            : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                }
            }
        });

        tcActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnToggle = new Button("🔒");
            private final HBox box = new HBox(6, btnEdit, btnToggle);

            {
                box.setAlignment(Pos.CENTER);
                btnEdit.setTooltip(new Tooltip("Modifier la section"));
                btnToggle.setTooltip(new Tooltip("Activer / Désactiver la section"));

                btnEdit.setOnAction(e -> {
                    Section s = getTableView().getItems().get(getIndex());
                    if (s != null) {
                        editSection(s);
                    }
                });

                btnToggle.setOnAction(e -> {
                    Section s = getTableView().getItems().get(getIndex());
                    if (s != null) {
                        s.setActive(!s.isActive());
                        academicService.saveSection(s);
                        loadSections();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableSections.setItems(sectionList);
    }

    private void loadSections() {
        sectionList.setAll(academicService.findAllSections());
    }

    private void editSection(Section s) {
        currentEditingSection = s;
        lblFormTitle.setText("Modifier la Section : " + s.getName());
        txtCode.setText(s.getCode());
        txtName.setText(s.getName());
        txtDescription.setText(s.getDescription());
        chkActive.setSelected(s.isActive());
        hideMessage();
    }

    @FXML
    private void handleSave() {
        if (!RoleGuard.can("academic.manage")) {
            showMessage("⛔ Permission insuffisante pour modifier les sections.", false);
            return;
        }

        if (btnSave != null) btnSave.setDisable(true);
        try {
            String code = txtCode.getText() != null ? txtCode.getText().trim() : "";
            String name = txtName.getText() != null ? txtName.getText().trim() : "";
            String desc = txtDescription.getText() != null ? txtDescription.getText().trim() : "";
            boolean active = chkActive.isSelected();

            if (code.isBlank() || name.isBlank()) {
                showMessage("Le code et le nom de la section sont obligatoires.", false);
                return;
            }

            if (currentEditingSection == null) {
                Section newSec = new Section(0, name, code, desc);
                newSec.setActive(active);
                academicService.saveSection(newSec);
                showMessage("✓ Section créée avec succès.", true);
            } else {
                currentEditingSection.setCode(code);
                currentEditingSection.setName(name);
                currentEditingSection.setDescription(desc);
                currentEditingSection.setActive(active);
                academicService.saveSection(currentEditingSection);
                showMessage("✓ Section mise à jour avec succès.", true);
            }

            handleReset();
            loadSections();
        } finally {
            if (btnSave != null) btnSave.setDisable(false);
        }
    }

    @FXML
    private void handleReset() {
        currentEditingSection = null;
        lblFormTitle.setText("Nouvelle Section");
        txtCode.clear();
        txtName.clear();
        txtDescription.clear();
        chkActive.setSelected(true);
        hideMessage();
    }

    private void showMessage(String msg, boolean success) {
        lblMessage.setText(msg);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
        if (success) {
            lblMessage.setStyle("-fx-background-color: #d1e7dd; -fx-text-fill: #0f5132; -fx-padding: 8; -fx-background-radius: 4;");
        } else {
            lblMessage.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-padding: 8; -fx-background-radius: 4;");
        }
    }

    private void hideMessage() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }
}
