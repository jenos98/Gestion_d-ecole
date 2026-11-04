package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.AcademicOption;
import com.jenos.gestionecole.model.Promotion;
import com.jenos.gestionecole.model.Section;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.service.OptionService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

/**
 * OptionsController — Gestion de la structure des options d'étude.
 */
public class OptionsController {

    // ── TableView ──
    @FXML private TableView<AcademicOption> tableOptions;
    @FXML private TableColumn<AcademicOption, String> tcOptionName;
    @FXML private TableColumn<AcademicOption, String> tcSection;
    @FXML private TableColumn<AcademicOption, String> tcPromotion;
    @FXML private TableColumn<AcademicOption, String> tcStatus;
    @FXML private TableColumn<AcademicOption, Void> tcActions;

    // ── Formulaire d'ajout / modification ──
    @FXML private TextField txtOptionName;
    @FXML private ComboBox<Section> cmbSection;
    @FXML private ComboBox<Promotion> cmbPromotion;
    @FXML private CheckBox chkActive;
    @FXML private Button btnSave;
    @FXML private Button btnResetForm;
    @FXML private Label lblFormTitle;
    @FXML private Label lblMessage;

    private final OptionService optionService = OptionService.getInstance();
    private final AcademicStructureService structureService = AcademicStructureService.getInstance();
    private final ObservableList<AcademicOption> optionList = FXCollections.observableArrayList();
    private AcademicOption selectedOption;

    @FXML
    public void initialize() {
        setupTable();
        setupForm();
        loadOptions();
    }

    private void setupTable() {
        tcOptionName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tcSection.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPromotion() != null && c.getValue().getPromotion().getSection() != null
                        ? c.getValue().getPromotion().getSection().getName() : "—"
        ));
        tcPromotion.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPromotion() != null ? c.getValue().getPromotion().getName() : "—"
        ));
        tcStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Active" : "Inactive"));

        tcStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add("badge");
                    lbl.getStyleClass().add("Active".equalsIgnoreCase(item) ? "badge-success" : "badge-neutral");
                    setGraphic(lbl);
                }
            }
        });

        tcActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnToggle = new Button("Statut");
            private final HBox box = new HBox(6, btnEdit, btnToggle);

            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnToggle.getStyleClass().add("btn-secondary");
                box.setAlignment(Pos.CENTER);

                btnEdit.setOnAction(e -> {
                    AcademicOption opt = getTableView().getItems().get(getIndex());
                    editOption(opt);
                });

                btnToggle.setOnAction(e -> {
                    AcademicOption opt = getTableView().getItems().get(getIndex());
                    optionService.toggleActive(opt.getId());
                    loadOptions();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableOptions.setItems(optionList);
    }

    private void setupForm() {
        cmbSection.setItems(FXCollections.observableArrayList(structureService.findAllSections()));
        cmbSection.valueProperty().addListener((obs, o, section) -> {
            cmbPromotion.getItems().clear();
            if (section != null) {
                cmbPromotion.setItems(FXCollections.observableArrayList(structureService.findPromotionsBySection(section)));
            }
        });
        chkActive.setSelected(true);
    }

    @FXML
    private void handleSave() {
        hideMessage();
        String name = txtOptionName.getText() != null ? txtOptionName.getText().trim() : "";
        Promotion promo = cmbPromotion.getValue();

        try {
            AcademicOption opt = selectedOption != null ? selectedOption : new AcademicOption();
            opt.setName(name);
            opt.setPromotion(promo);
            opt.setActive(chkActive.isSelected());

            optionService.save(opt);

            showMessage("Option enregistrée avec succès !", false);
            resetForm();
            loadOptions();
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), true);
        }
    }

    @FXML
    private void handleReset() {
        resetForm();
    }

    private void editOption(AcademicOption opt) {
        this.selectedOption = opt;
        lblFormTitle.setText("Modifier l'Option : " + opt.getName());
        txtOptionName.setText(opt.getName());
        if (opt.getPromotion() != null) {
            cmbSection.setValue(opt.getPromotion().getSection());
            cmbPromotion.setValue(opt.getPromotion());
        }
        chkActive.setSelected(opt.isActive());
        btnSave.setText("Mettre à jour");
    }

    private void resetForm() {
        selectedOption = null;
        lblFormTitle.setText("Nouvelle Option d'Étude");
        txtOptionName.clear();
        cmbSection.setValue(null);
        cmbPromotion.setValue(null);
        chkActive.setSelected(true);
        btnSave.setText("Enregistrer l'Option");
    }

    private void loadOptions() {
        optionList.setAll(optionService.findAll());
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
