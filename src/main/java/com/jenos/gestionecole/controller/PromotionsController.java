package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.Promotion;
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

import java.util.List;

/**
 * PromotionsController — Contrôleur de gestion des promotions (promotions.fxml).
 */
public class PromotionsController {

    @FXML private Label lblFormTitle;
    @FXML private Label lblMessage;
    @FXML private ComboBox<Section> cmbSection;
    @FXML private TextField txtCode;
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private CheckBox chkActive;

    @FXML private ComboBox<Section> cmbFilterSection;
    @FXML private TableView<Promotion> tablePromotions;
    @FXML private TableColumn<Promotion, String> tcCode;
    @FXML private TableColumn<Promotion, String> tcName;
    @FXML private TableColumn<Promotion, String> tcSection;
    @FXML private TableColumn<Promotion, String> tcDescription;
    @FXML private TableColumn<Promotion, String> tcStatus;
    @FXML private TableColumn<Promotion, Void> tcActions;

    private final AcademicStructureService academicService = AcademicStructureService.getInstance();
    private final ObservableList<Promotion> promotionList = FXCollections.observableArrayList();
    private Promotion currentEditingPromotion = null;

    @FXML
    public void initialize() {
        setupCombos();
        setupTable();
        loadPromotions();
    }

    private void setupCombos() {
        List<Section> sections = academicService.findAllSections();
        cmbSection.setItems(FXCollections.observableArrayList(sections));

        ObservableList<Section> filterSections = FXCollections.observableArrayList();
        filterSections.addAll(sections);
        cmbFilterSection.setItems(filterSections);
        cmbFilterSection.valueProperty().addListener((obs, o, s) -> filterPromotions(s));
    }

    private void setupTable() {
        tcCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        tcName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tcSection.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSection() != null ? c.getValue().getSection().getName() : "—"));
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
                btnEdit.setTooltip(new Tooltip("Modifier la promotion"));
                btnToggle.setTooltip(new Tooltip("Activer / Désactiver la promotion"));

                btnEdit.setOnAction(e -> {
                    Promotion p = getTableView().getItems().get(getIndex());
                    if (p != null) {
                        editPromotion(p);
                    }
                });

                btnToggle.setOnAction(e -> {
                    Promotion p = getTableView().getItems().get(getIndex());
                    if (p != null) {
                        p.setActive(!p.isActive());
                        academicService.savePromotion(p);
                        loadPromotions();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablePromotions.setItems(promotionList);
    }

    private void loadPromotions() {
        filterPromotions(cmbFilterSection.getValue());
    }

    private void filterPromotions(Section filterSection) {
        if (filterSection == null) {
            promotionList.setAll(academicService.findAllPromotions());
        } else {
            promotionList.setAll(academicService.findPromotionsBySection(filterSection));
        }
    }

    private void editPromotion(Promotion p) {
        currentEditingPromotion = p;
        lblFormTitle.setText("Modifier la Promotion : " + p.getName());
        txtCode.setText(p.getCode());
        txtName.setText(p.getName());
        txtDescription.setText(p.getDescription());
        cmbSection.setValue(p.getSection());
        chkActive.setSelected(p.isActive());
        hideMessage();
    }

    @FXML
    private void handleSave() {
        if (!RoleGuard.can("academic.manage")) {
            showMessage("⛔ Permission insuffisante pour configurer les promotions.", false);
            return;
        }

        Section sec = cmbSection.getValue();
        String code = txtCode.getText() != null ? txtCode.getText().trim() : "";
        String name = txtName.getText() != null ? txtName.getText().trim() : "";
        String desc = txtDescription.getText() != null ? txtDescription.getText().trim() : "";
        boolean active = chkActive.isSelected();

        if (sec == null || code.isBlank() || name.isBlank()) {
            showMessage("La section, le code et le nom sont obligatoires.", false);
            return;
        }

        if (currentEditingPromotion == null) {
            Promotion newPromo = new Promotion(0, name, code, sec, desc);
            newPromo.setActive(active);
            academicService.savePromotion(newPromo);
            showMessage("✓ Promotion créée avec succès.", true);
        } else {
            currentEditingPromotion.setSection(sec);
            currentEditingPromotion.setCode(code);
            currentEditingPromotion.setName(name);
            currentEditingPromotion.setDescription(desc);
            currentEditingPromotion.setActive(active);
            academicService.savePromotion(currentEditingPromotion);
            showMessage("✓ Promotion mise à jour avec succès.", true);
        }

        handleReset();
        loadPromotions();
    }

    @FXML
    private void handleReset() {
        currentEditingPromotion = null;
        lblFormTitle.setText("Nouvelle Promotion");
        txtCode.clear();
        txtName.clear();
        txtDescription.clear();
        cmbSection.setValue(null);
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
