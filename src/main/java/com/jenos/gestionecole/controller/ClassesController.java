package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.AcademicOption;
import com.jenos.gestionecole.model.Classe;
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
 * ClassesController — Contrôleur de gestion des classes (classes.fxml).
 * Respecte strictement la hiérarchie : Section → Promotion → Option → Classe.
 */
public class ClassesController {

    @FXML private Label lblFormTitle;
    @FXML private Label lblMessage;

    @FXML private ComboBox<Section> cmbSection;
    @FXML private ComboBox<Promotion> cmbPromotion;
    @FXML private ComboBox<AcademicOption> cmbOption;

    @FXML private TextField txtName;
    @FXML private TextField txtCode;
    @FXML private TextField txtTitulaire;
    @FXML private TextField txtCapacity;
    @FXML private CheckBox chkActive;
    @FXML private Button btnReset;
    @FXML private Button btnSave;

    @FXML private ComboBox<Section> cmbFilterSection;
    @FXML private ComboBox<AcademicOption> cmbFilterOption;

    @FXML private TableView<Classe> tableClasses;
    @FXML private TableColumn<Classe, String> tcCode;
    @FXML private TableColumn<Classe, String> tcName;
    @FXML private TableColumn<Classe, String> tcPromotion;
    @FXML private TableColumn<Classe, String> tcOption;
    @FXML private TableColumn<Classe, String> tcTitulaire;
    @FXML private TableColumn<Classe, String> tcCapacity;
    @FXML private TableColumn<Classe, String> tcStatus;
    @FXML private TableColumn<Classe, Void> tcActions;

    private final AcademicStructureService academicService = AcademicStructureService.getInstance();
    private final ObservableList<Classe> classeList = FXCollections.observableArrayList();
    private Classe currentEditingClasse = null;

    @FXML
    public void initialize() {
        setupCombos();
        setupTable();
        loadClasses();
    }

    private void setupCombos() {
        List<Section> sections = academicService.findAllSections();
        cmbSection.setItems(FXCollections.observableArrayList(sections));

        // Cascade Section → Promotion
        cmbSection.valueProperty().addListener((obs, oldSec, sec) -> {
            cmbPromotion.getItems().clear();
            cmbOption.getItems().clear();
            if (sec != null) {
                cmbPromotion.setItems(FXCollections.observableArrayList(academicService.findPromotionsBySection(sec)));
            }
        });

        // Cascade Promotion → Option
        cmbPromotion.valueProperty().addListener((obs, oldP, promo) -> {
            cmbOption.getItems().clear();
            if (promo != null) {
                cmbOption.setItems(FXCollections.observableArrayList(academicService.findOptionsByPromotion(promo)));
            }
        });

        // Filtres tableau
        cmbFilterSection.setItems(FXCollections.observableArrayList(sections));
        cmbFilterSection.valueProperty().addListener((obs, o, s) -> filterClasses());

        cmbFilterOption.setItems(FXCollections.observableArrayList(academicService.findAllOptions()));
        cmbFilterOption.valueProperty().addListener((obs, o, opt) -> filterClasses());
    }

    private void setupTable() {
        tcCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        tcName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tcPromotion.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPromotion() != null ? c.getValue().getPromotion().getName() : "—"));
        tcOption.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOption() != null ? c.getValue().getOption().getName() : "Sans option"));
        tcTitulaire.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitulaire()));
        tcCapacity.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCapacity())));

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
                btnEdit.setTooltip(new Tooltip("Modifier la classe"));
                btnToggle.setTooltip(new Tooltip("Activer / Désactiver la classe"));

                btnEdit.setOnAction(e -> {
                    Classe c = getTableView().getItems().get(getIndex());
                    if (c != null) {
                        editClasse(c);
                    }
                });

                btnToggle.setOnAction(e -> {
                    Classe c = getTableView().getItems().get(getIndex());
                    if (c != null) {
                        c.setActive(!c.isActive());
                        academicService.saveClasse(c);
                        loadClasses();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableClasses.setItems(classeList);
    }

    private void loadClasses() {
        filterClasses();
    }

    private void filterClasses() {
        Section sec = cmbFilterSection.getValue();
        AcademicOption opt = cmbFilterOption.getValue();

        List<Classe> all = academicService.findAllClasses();
        List<Classe> filtered = all.stream()
                .filter(c -> sec == null || (c.getSection() != null && c.getSection().getId() == sec.getId()))
                .filter(c -> opt == null || (c.getOption() != null && c.getOption().getId() == opt.getId()))
                .toList();

        classeList.setAll(filtered);
    }

    private void editClasse(Classe c) {
        currentEditingClasse = c;
        lblFormTitle.setText("Modifier la Classe : " + c.getName());
        txtCode.setText(c.getCode());
        txtName.setText(c.getName());
        txtTitulaire.setText(c.getTitulaire());
        txtCapacity.setText(String.valueOf(c.getCapacity()));
        chkActive.setSelected(c.isActive());

        if (c.getSection() != null) cmbSection.setValue(c.getSection());
        if (c.getPromotion() != null) cmbPromotion.setValue(c.getPromotion());
        if (c.getOption() != null) cmbOption.setValue(c.getOption());

        hideMessage();
    }

    @FXML
    private void handleSave() {
        if (!RoleGuard.can("academic.manage")) {
            showMessage("⛔ Permission insuffisante pour configurer les classes.", false);
            return;
        }

        if (btnSave != null) btnSave.setDisable(true);
        try {
            Section sec = cmbSection.getValue();
            Promotion promo = cmbPromotion.getValue();
            AcademicOption opt = cmbOption.getValue();
            String name = txtName.getText() != null ? txtName.getText().trim() : "";
            String code = txtCode.getText() != null ? txtCode.getText().trim() : "";
            String titulaire = txtTitulaire.getText() != null ? txtTitulaire.getText().trim() : "Non assigné";

            int cap = 45;
            try {
                if (txtCapacity.getText() != null && !txtCapacity.getText().isBlank()) {
                    cap = Integer.parseInt(txtCapacity.getText().trim());
                }
            } catch (NumberFormatException ignored) {}

            if (sec == null || promo == null || name.isBlank() || code.isBlank()) {
                showMessage("La section, la promotion, le nom et le code sont obligatoires.", false);
                return;
            }

            // Validation hiérarchique stricte
            if (promo.getSection() != null && promo.getSection().getId() != sec.getId()) {
                showMessage("Incohérence hiérarchique : La promotion n'appartient pas à la section sélectionnée.", false);
                return;
            }
            if (opt != null && opt.getPromotion() != null && opt.getPromotion().getId() != promo.getId()) {
                showMessage("Incohérence hiérarchique : L'option n'appartient pas à la promotion sélectionnée.", false);
                return;
            }

            if (currentEditingClasse == null) {
                Classe newCls = new Classe(0, name, code, opt, promo, titulaire, cap);
                newCls.setActive(chkActive.isSelected());
                academicService.saveClasse(newCls);
                showMessage("✓ Classe créée avec succès.", true);
            } else {
                currentEditingClasse.setName(name);
                currentEditingClasse.setCode(code);
                currentEditingClasse.setTitulaire(titulaire);
                currentEditingClasse.setCapacity(cap);
                currentEditingClasse.setPromotion(promo);
                currentEditingClasse.setOption(opt);
                currentEditingClasse.setActive(chkActive.isSelected());
                academicService.saveClasse(currentEditingClasse);
                showMessage("✓ Classe mise à jour avec succès.", true);
            }

            handleReset();
            loadClasses();
        } finally {
            if (btnSave != null) btnSave.setDisable(false);
        }
    }

    @FXML
    private void handleReset() {
        currentEditingClasse = null;
        lblFormTitle.setText("Nouvelle Classe");
        txtCode.clear();
        txtName.clear();
        txtTitulaire.clear();
        txtCapacity.setText("45");
        cmbSection.setValue(null);
        cmbPromotion.setValue(null);
        cmbOption.setValue(null);
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
