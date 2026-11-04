package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.service.AcademicStructureService;
import com.jenos.gestionecole.service.FeeService;
import com.jenos.gestionecole.util.MoneyFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FeesController — Contrôleur de la grille tarifaire et configuration des frais scolaires.
 *
 * RÈGLE ABSOLUE : Les montants USD et CDF sont traités séparément,
 * JAMAIS additionnés ni convertis implicitement.
 */
public class FeesController {

    // ── En-tête & Messages ──
    @FXML private Label lblMessage;

    // ── Formulaire (Volet Gauche) ──
    @FXML private Label lblFormTitle;
    @FXML private TextField txtFeeName;
    @FXML private ComboBox<Fee.FeeCategory> cmbCategory;
    @FXML private TextField txtAmount;
    @FXML private ComboBox<String> cmbCurrency;
    @FXML private ComboBox<Fee.ScopeLevel> cmbScopeLevel;

    // Conteneurs de cascade
    @FXML private VBox boxCascadeContainer;
    @FXML private VBox boxSection;
    @FXML private VBox boxPromotion;
    @FXML private VBox boxOption;
    @FXML private VBox boxClasse;

    @FXML private ComboBox<Section> cmbSection;
    @FXML private ComboBox<Promotion> cmbPromotion;
    @FXML private ComboBox<AcademicOption> cmbOption;
    @FXML private ComboBox<Classe> cmbClasse;

    @FXML private DatePicker dpEcheance;
    @FXML private ComboBox<String> cmbAnneeScolaire;
    @FXML private CheckBox chkMandatory;
    @FXML private CheckBox chkActive;

    @FXML private Button btnSaveFee;
    @FXML private Button btnResetForm;

    // ── Tableau (Volet Droit) ──
    @FXML private ComboBox<String> cmbFilterCategory;
    @FXML private TableView<Fee> tableFees;
    @FXML private TableColumn<Fee, String> tcName;
    @FXML private TableColumn<Fee, String> tcCategory;
    @FXML private TableColumn<Fee, String> tcAmount;
    @FXML private TableColumn<Fee, String> tcCurrency;
    @FXML private TableColumn<Fee, String> tcTarget;
    @FXML private TableColumn<Fee, String> tcEcheance;
    @FXML private TableColumn<Fee, String> tcMandatory;
    @FXML private TableColumn<Fee, String> tcStatus;
    @FXML private TableColumn<Fee, Void> tcActions;

    // ── Synthèse des totaux ──
    @FXML private Label lblTotalConfiguredUSD;
    @FXML private Label lblTotalConfiguredCDF;

    private final FeeService feeService = FeeService.getInstance();
    private final AcademicStructureService structureService = AcademicStructureService.getInstance();
    private final ObservableList<Fee> feeList = FXCollections.observableArrayList();
    private Fee selectedFee;

    @FXML
    public void initialize() {
        setupForm();
        setupTable();
        setupCascade();
        loadFees();
    }

    private void setupForm() {
        cmbCategory.setItems(FXCollections.observableArrayList(Fee.FeeCategory.values()));
        cmbCategory.setValue(Fee.FeeCategory.MINERVAL_T1);

        cmbCurrency.setItems(FXCollections.observableArrayList("USD", "CDF"));
        cmbCurrency.setValue("USD");

        cmbScopeLevel.setItems(FXCollections.observableArrayList(Fee.ScopeLevel.values()));
        cmbScopeLevel.setValue(Fee.ScopeLevel.GLOBAL);

        cmbScopeLevel.valueProperty().addListener((obs, o, scope) -> updateCascadeVisibility(scope));
        updateCascadeVisibility(Fee.ScopeLevel.GLOBAL);

        cmbAnneeScolaire.setItems(FXCollections.observableArrayList(structureService.findAllAcademicYears()));
        if (!cmbAnneeScolaire.getItems().isEmpty()) {
            cmbAnneeScolaire.setValue(cmbAnneeScolaire.getItems().get(0));
        }

        dpEcheance.setValue(LocalDate.now().plusMonths(1));
    }

    private void updateCascadeVisibility(Fee.ScopeLevel scope) {
        if (scope == null || scope == Fee.ScopeLevel.GLOBAL) {
            boxCascadeContainer.setVisible(false);
            boxCascadeContainer.setManaged(false);
            return;
        }

        boxCascadeContainer.setVisible(true);
        boxCascadeContainer.setManaged(true);

        boolean showSec = (scope == Fee.ScopeLevel.SECTION || scope == Fee.ScopeLevel.PROMOTION
                || scope == Fee.ScopeLevel.OPTION || scope == Fee.ScopeLevel.CLASSE);
        boolean showPromo = (scope == Fee.ScopeLevel.PROMOTION || scope == Fee.ScopeLevel.OPTION || scope == Fee.ScopeLevel.CLASSE);
        boolean showOpt = (scope == Fee.ScopeLevel.OPTION || scope == Fee.ScopeLevel.CLASSE);
        boolean showCl = (scope == Fee.ScopeLevel.CLASSE);

        boxSection.setVisible(showSec);
        boxSection.setManaged(showSec);

        boxPromotion.setVisible(showPromo);
        boxPromotion.setManaged(showPromo);

        boxOption.setVisible(showOpt);
        boxOption.setManaged(showOpt);

        boxClasse.setVisible(showCl);
        boxClasse.setManaged(showCl);
    }

    private void setupCascade() {
        cmbSection.setItems(FXCollections.observableArrayList(structureService.findAllSections()));

        cmbSection.valueProperty().addListener((obs, o, sec) -> {
            cmbPromotion.getItems().clear();
            cmbOption.getItems().clear();
            cmbClasse.getItems().clear();
            if (sec != null) {
                cmbPromotion.setItems(FXCollections.observableArrayList(structureService.findPromotionsBySection(sec)));
            }
        });

        cmbPromotion.valueProperty().addListener((obs, o, promo) -> {
            cmbOption.getItems().clear();
            cmbClasse.getItems().clear();
            if (promo != null) {
                cmbOption.setItems(FXCollections.observableArrayList(structureService.findOptionsByPromotion(promo)));
                if (structureService.findOptionsByPromotion(promo).isEmpty()) {
                    cmbClasse.setItems(FXCollections.observableArrayList(structureService.findClassesByPromotion(promo)));
                }
            }
        });

        cmbOption.valueProperty().addListener((obs, o, opt) -> {
            cmbClasse.getItems().clear();
            if (opt != null) {
                cmbClasse.setItems(FXCollections.observableArrayList(structureService.findClassesByOption(opt)));
            }
        });
    }

    private void setupTable() {
        tcName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tcCategory.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategory() != null ? c.getValue().getCategory().getLabel() : "—"));

        tcAmount.setCellValueFactory(c -> new SimpleStringProperty(
                "USD".equalsIgnoreCase(c.getValue().getCurrency())
                        ? String.format("%.2f", c.getValue().getAmount())
                        : String.format("%,.0f", c.getValue().getAmount())
        ));

        tcCurrency.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrency()));
        tcCurrency.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String cur, boolean empty) {
                super.updateItem(cur, empty);
                if (empty || cur == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(cur);
                    lbl.getStyleClass().add("badge");
                    lbl.getStyleClass().add("USD".equalsIgnoreCase(cur) ? "badge-success" : "badge-neutral");
                    setGraphic(lbl);
                }
            }
        });

        tcTarget.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTargetDescription()));
        tcEcheance.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateEcheance() != null ? c.getValue().getDateEcheance().toString() : "—"));

        tcMandatory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isMandatory() ? "Oui" : "Optionnel"));

        tcStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Actif" : "Inactif"));
        tcStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String st, boolean empty) {
                super.updateItem(st, empty);
                if (empty || st == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(st);
                    lbl.getStyleClass().add("badge");
                    lbl.getStyleClass().add("Actif".equalsIgnoreCase(st) ? "badge-success" : "badge-danger");
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
                    Fee f = getTableView().getItems().get(getIndex());
                    editFee(f);
                });

                btnToggle.setOnAction(e -> {
                    Fee f = getTableView().getItems().get(getIndex());
                    feeService.toggleActive(f.getId());
                    loadFees();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableFees.setItems(feeList);

        // Filtre catégorie
        cmbFilterCategory.getItems().add("Toutes les catégories");
        for (Fee.FeeCategory cat : Fee.FeeCategory.values()) {
            cmbFilterCategory.getItems().add(cat.getLabel());
        }
        cmbFilterCategory.setValue("Toutes les catégories");
        cmbFilterCategory.valueProperty().addListener((obs, o, val) -> applyCategoryFilter(val));
    }

    private void applyCategoryFilter(String categoryLabel) {
        if (categoryLabel == null || "Toutes les catégories".equalsIgnoreCase(categoryLabel)) {
            feeList.setAll(feeService.findAll());
        } else {
            List<Fee> filtered = feeService.findAll().stream()
                    .filter(f -> f.getCategory() != null && categoryLabel.equalsIgnoreCase(f.getCategory().getLabel()))
                    .collect(Collectors.toList());
            feeList.setAll(filtered);
        }
        updateTotals();
    }

    @FXML
    private void handleSave() {
        hideMessage();
        String name = txtFeeName.getText() != null ? txtFeeName.getText().trim() : "";
        String strAmount = txtAmount.getText() != null ? txtAmount.getText().trim().replace(",", ".") : "";

        if (name.isEmpty()) {
            showMessage("Le libellé du frais est obligatoire.", true);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(strAmount);
            if (amount <= 0) {
                showMessage("Le montant doit être strictement supérieur à zéro.", true);
                return;
            }
        } catch (NumberFormatException e) {
            showMessage("Montant invalide. Veuillez entrer un nombre valide.", true);
            return;
        }

        Fee.ScopeLevel scope = cmbScopeLevel.getValue();

        try {
            Fee fee = (selectedFee != null) ? selectedFee : new Fee();
            fee.setName(name);
            fee.setCategory(cmbCategory.getValue());
            fee.setAmount(amount);
            fee.setCurrency(cmbCurrency.getValue());
            fee.setScopeLevel(scope);
            fee.setAnneeScolaire(cmbAnneeScolaire.getValue());
            fee.setDateEcheance(dpEcheance.getValue());
            fee.setMandatory(chkMandatory.isSelected());
            fee.setActive(chkActive.isSelected());

            if (scope != null) {
                switch (scope) {
                    case SECTION -> fee.setSection(cmbSection.getValue());
                    case PROMOTION -> fee.setPromotion(cmbPromotion.getValue());
                    case OPTION -> fee.setOption(cmbOption.getValue());
                    case CLASSE -> fee.setClasse(cmbClasse.getValue());
                    case GLOBAL -> {}
                }
            }

            feeService.save(fee);

            showMessage("Frais scolaire enregistré avec succès !", false);
            resetForm();
            loadFees();
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), true);
        }
    }

    @FXML
    private void handleReset() {
        resetForm();
    }

    private void editFee(Fee fee) {
        this.selectedFee = fee;
        lblFormTitle.setText("Modifier le Frais : " + fee.getName());
        txtFeeName.setText(fee.getName());
        cmbCategory.setValue(fee.getCategory());
        txtAmount.setText(String.valueOf(fee.getAmount()));
        cmbCurrency.setValue(fee.getCurrency());
        cmbScopeLevel.setValue(fee.getScopeLevel());
        dpEcheance.setValue(fee.getDateEcheance());
        cmbAnneeScolaire.setValue(fee.getAnneeScolaire());
        chkMandatory.setSelected(fee.isMandatory());
        chkActive.setSelected(fee.isActive());

        if (fee.getSection() != null) cmbSection.setValue(fee.getSection());
        if (fee.getPromotion() != null) cmbPromotion.setValue(fee.getPromotion());
        if (fee.getOption() != null) cmbOption.setValue(fee.getOption());
        if (fee.getClasse() != null) cmbClasse.setValue(fee.getClasse());

        btnSaveFee.setText("Mettre à jour");
    }

    private void resetForm() {
        selectedFee = null;
        lblFormTitle.setText("Nouveau Frais Scolaire");
        txtFeeName.clear();
        txtAmount.clear();
        cmbCategory.setValue(Fee.FeeCategory.MINERVAL_T1);
        cmbCurrency.setValue("USD");
        cmbScopeLevel.setValue(Fee.ScopeLevel.GLOBAL);
        cmbSection.setValue(null);
        cmbPromotion.setValue(null);
        cmbOption.setValue(null);
        cmbClasse.setValue(null);
        chkMandatory.setSelected(true);
        chkActive.setSelected(true);
        dpEcheance.setValue(LocalDate.now().plusMonths(1));
        btnSaveFee.setText("Enregistrer le Frais");
    }

    private void loadFees() {
        feeList.setAll(feeService.findAll());
        updateTotals();
    }

    private void updateTotals() {
        double totalUSD = 0;
        double totalCDF = 0;

        for (Fee f : feeList) {
            if (f.isActive()) {
                if ("USD".equalsIgnoreCase(f.getCurrency())) {
                    totalUSD += f.getAmount();
                } else if ("CDF".equalsIgnoreCase(f.getCurrency())) {
                    totalCDF += f.getAmount();
                }
            }
        }

        lblTotalConfiguredUSD.setText(String.format("%.2f USD", totalUSD));
        lblTotalConfiguredCDF.setText(String.format("%,.0f CDF", totalCDF));
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
