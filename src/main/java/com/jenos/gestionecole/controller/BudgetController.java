package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.AcademicYear;
import com.jenos.gestionecole.model.Budget;
import com.jenos.gestionecole.service.AcademicYearService;
import com.jenos.gestionecole.service.BudgetService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.RoleGuard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.stream.Collectors;

/**
 * BudgetController — Contrôleur de gestion et supervision budgétaire (budget.fxml).
 * Intègre MoneyVisibilityManager et alertes par paliers (80%, 90%, 100%).
 */
public class BudgetController implements MoneyVisibilityManager.VisibilityObserver {

    @FXML private Button btnToggleVisibility;
    @FXML private Button btnNewBudget;

    // Cartes globales USD et CDF
    @FXML private Label lblAllocatedUSD;
    @FXML private Label lblSpentUSD;
    @FXML private Label lblRemainingUSD;

    @FXML private Label lblAllocatedCDF;
    @FXML private Label lblSpentCDF;
    @FXML private Label lblRemainingCDF;

    // Formulaire
    @FXML private Label lblFormTitle;
    @FXML private Label lblFormMessage;
    @FXML private TextField txtBudgetName;
    @FXML private TextField txtBudgetCode;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField txtAmount;
    @FXML private ComboBox<String> cmbCurrency;
    @FXML private ComboBox<String> cmbFiscalYear;

    // Tableau
    @FXML private TableView<Budget> tableBudgets;
    @FXML private TableColumn<Budget, String> tcCode;
    @FXML private TableColumn<Budget, String> tcName;
    @FXML private TableColumn<Budget, String> tcCategory;
    @FXML private TableColumn<Budget, String> tcCurrency;
    @FXML private TableColumn<Budget, String> tcAllocated;
    @FXML private TableColumn<Budget, String> tcSpent;
    @FXML private TableColumn<Budget, String> tcRemaining;
    @FXML private TableColumn<Budget, String> tcRate;
    @FXML private TableColumn<Budget, String> tcStatus;

    private final BudgetService budgetService = BudgetService.getInstance();
    private final AcademicYearService yearService = AcademicYearService.getInstance();
    private final ObservableList<Budget> budgetList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        MoneyVisibilityManager.getInstance().registerObserver(this);
        setupCombos();
        setupTable();
        loadBudgets();
        updateSummaryCards();
    }

    private void setupCombos() {
        cmbCategory.setItems(FXCollections.observableArrayList(
                "Pédagogique", "Infrastructure", "Logistique", "Vie Scolaire", "Santé", "Administration"
        ));
        cmbCurrency.setItems(FXCollections.observableArrayList("USD", "CDF"));
        cmbCurrency.setValue("USD");

        var years = yearService.getAllYears().stream().map(AcademicYear::getCode).collect(Collectors.toList());
        cmbFiscalYear.setItems(FXCollections.observableArrayList(years));
        AcademicYear active = yearService.getActiveYear();
        if (active != null) cmbFiscalYear.setValue(active.getCode());
    }

    private void setupTable() {
        tcCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCode()));
        tcName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tcCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        tcCurrency.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrency()));

        tcAllocated.setCellValueFactory(c -> new SimpleStringProperty(
                MoneyFormatter.display(c.getValue().getAllocatedAmount(), c.getValue().getCurrency())));
        tcSpent.setCellValueFactory(c -> new SimpleStringProperty(
                MoneyFormatter.display(c.getValue().getSpentAmount(), c.getValue().getCurrency())));
        tcRemaining.setCellValueFactory(c -> new SimpleStringProperty(
                MoneyFormatter.display(c.getValue().getAvailableAmount(), c.getValue().getCurrency())));

        // Jauge et taux
        tcRate.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.1f %%", c.getValue().getConsumptionRate())));
        tcRate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Budget b = getTableView().getItems().get(getIndex());
                    setText(item);
                    if (b != null) {
                        if (b.isExceeded100()) {
                            setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // Rouge (≥100%)
                        } else if (b.isWarning90()) {
                            setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;"); // Orange foncé (≥90%)
                        } else if (b.isWarning80()) {
                            setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;"); // Orange (≥80%)
                        } else {
                            setStyle("-fx-text-fill: #198754;"); // Vert (<80%)
                        }
                    }
                }
            }
        });

        tcStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        tcStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("DEPASSE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                    } else if ("ACTIF".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #198754; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #6c757d;");
                    }
                }
            }
        });

        tableBudgets.setItems(budgetList);
    }

    private void loadBudgets() {
        budgetList.setAll(budgetService.getAllBudgets());
        tableBudgets.refresh();
    }

    private void updateSummaryCards() {
        double allocUSD = budgetService.calculateTotalAllocatedUSD();
        double spentUSD = budgetService.calculateTotalSpentUSD();
        double remUSD = allocUSD - spentUSD;

        double allocCDF = budgetService.calculateTotalAllocatedCDF();
        double spentCDF = budgetService.calculateTotalSpentCDF();
        double remCDF = allocCDF - spentCDF;

        lblAllocatedUSD.setText(MoneyFormatter.display(allocUSD, "USD"));
        lblSpentUSD.setText(MoneyFormatter.display(spentUSD, "USD"));
        lblRemainingUSD.setText(MoneyFormatter.display(remUSD, "USD"));

        lblAllocatedCDF.setText(MoneyFormatter.display(allocCDF, "CDF"));
        lblSpentCDF.setText(MoneyFormatter.display(spentCDF, "CDF"));
        lblRemainingCDF.setText(MoneyFormatter.display(remCDF, "CDF"));

        if (MoneyVisibilityManager.getInstance().isVisible()) {
            btnToggleVisibility.setText("🙈 Masquer les montants");
        } else {
            btnToggleVisibility.setText("👁 Afficher les montants");
        }
    }

    @FXML
    private void handleToggleVisibility() {
        if (MoneyVisibilityManager.getInstance().isVisible()) {
            MoneyVisibilityManager.getInstance().hideAll();
        } else {
            MoneyVisibilityManager.getInstance().showAll();
        }
    }

    @Override
    public void onShow() {
        updateSummaryCards();
        tableBudgets.refresh();
    }

    @Override
    public void onHide() {
        updateSummaryCards();
        tableBudgets.refresh();
    }

    @FXML
    private void handleNewBudget() {
        handleResetForm();
    }

    @FXML
    private void handleSaveBudget() {
        if (!RoleGuard.can("budget.manage")) {
            showFormMessage("⛔ Permission insuffisante : Seul le Promoteur peut allouer des lignes budgétaires.", false);
            return;
        }

        String name = txtBudgetName.getText() != null ? txtBudgetName.getText().trim() : "";
        String code = txtBudgetCode.getText() != null ? txtBudgetCode.getText().trim() : "";
        String cat = cmbCategory.getValue();
        String cur = cmbCurrency.getValue();
        String year = cmbFiscalYear.getValue();

        double amount = 0.0;
        try {
            amount = Double.parseDouble(txtAmount.getText().trim());
        } catch (Exception ex) {
            showFormMessage("Veuillez saisir un montant alloué valide.", false);
            return;
        }

        if (name.isBlank() || code.isBlank() || cat == null || cur == null || year == null || amount <= 0) {
            showFormMessage("Veuillez remplir tous les champs obligatoires avec un montant supérieur à 0.", false);
            return;
        }

        try {
            Budget b = new Budget(0, name, code, cat, amount, 0.0, cur, year, "ACTIF");
            budgetService.addBudget(b);
            loadBudgets();
            updateSummaryCards();
            handleResetForm();
            showFormMessage("✓ Ligne budgétaire allouée avec succès.", true);
        } catch (Exception ex) {
            showFormMessage(ex.getMessage(), false);
        }
    }

    @FXML
    private void handleResetForm() {
        txtBudgetName.clear();
        txtBudgetCode.clear();
        txtAmount.clear();
        cmbCategory.setValue(null);
        hideFormMessage();
    }

    private void showFormMessage(String msg, boolean success) {
        lblFormMessage.setText(msg);
        lblFormMessage.setVisible(true);
        lblFormMessage.setManaged(true);
        if (success) {
            lblFormMessage.setStyle("-fx-background-color: #d1e7dd; -fx-text-fill: #0f5132; -fx-padding: 8; -fx-background-radius: 4;");
        } else {
            lblFormMessage.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-padding: 8; -fx-background-radius: 4;");
        }
    }

    private void hideFormMessage() {
        lblFormMessage.setVisible(false);
        lblFormMessage.setManaged(false);
    }
}
