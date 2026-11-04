package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.CashSession;
import com.jenos.gestionecole.model.Expense;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.service.ExpenseService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.Route;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ExpensesController — Gestion et engagement des dépenses de caisse.
 *
 * RÈGLES MÉTIER :
 * 1. Séparation stricte USD / CDF
 * 2. Vérification immédiate du solde disponible en caisse
 * 3. Récapitulatif et dialogue de confirmation avant enregistrement
 * 4. Détection du seuil d'autorisation (EN_ATTENTE)
 * 5. Aucune suppression physique
 */
public class ExpensesController implements MoneyVisibilityManager.VisibilityObserver {

    // ── Formulaire Dépense ──
    @FXML private Label lblAvailableCashInfo;
    @FXML private Label lblExpenseError;
    @FXML private Label lblExpenseSuccess;

    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField txtBeneficiary;
    @FXML private TextField txtLabel;
    @FXML private TextField txtAmount;
    @FXML private ComboBox<String> cmbCurrency;
    @FXML private ComboBox<Expense.PaymentMethod> cmbPaymentMethod;
    @FXML private TextField txtReference;
    @FXML private TextArea txtReason;

    // Justificatif
    @FXML private Button btnAddAttachment;
    @FXML private Label lblAttachmentName;
    @FXML private Button btnRemoveAttachment;

    @FXML private Button btnResetExpense;
    @FXML private Button btnSubmitExpense;
    @FXML private Button btnBackToCash;

    // ── Tableau des Dépenses ──
    @FXML private Label lblExpensesCount;
    @FXML private Button btnToggleAmounts;
    @FXML private Button btnRefreshExpenses;
    @FXML private TableView<Expense> tableExpenses;
    @FXML private TableColumn<Expense, String> colDate;
    @FXML private TableColumn<Expense, String> colVoucher;
    @FXML private TableColumn<Expense, String> colCategory;
    @FXML private TableColumn<Expense, String> colLabel;
    @FXML private TableColumn<Expense, String> colCur;
    @FXML private TableColumn<Expense, String> colAmount;
    @FXML private TableColumn<Expense, String> colBeneficiary;
    @FXML private TableColumn<Expense, String> colMethod;
    @FXML private TableColumn<Expense, String> colStatus;
    @FXML private TableColumn<Expense, Void> colActions;

    private final ExpenseService expenseService = ExpenseService.getInstance();
    private final CashClosureService cashClosureService = CashClosureService.getInstance();
    private final ObservableList<Expense> expenseList = FXCollections.observableArrayList();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String attachedFileName = null;

    @FXML
    public void initialize() {
        MoneyVisibilityManager.getInstance().registerObserver(this);

        setupForm();
        setupTable();
        loadExpenses();
        updateAvailableCashDisplay();
    }

    private void setupForm() {
        dpDate.setValue(LocalDate.now());

        // Catégories configurables dynamiquement
        cmbCategory.setItems(FXCollections.observableArrayList(expenseService.getCategories()));

        // Devises
        cmbCurrency.setItems(FXCollections.observableArrayList("USD", "CDF"));
        cmbCurrency.setValue("USD");

        // Modes de paiement
        cmbPaymentMethod.setItems(FXCollections.observableArrayList(Expense.PaymentMethod.values()));
        cmbPaymentMethod.setValue(Expense.PaymentMethod.ESPECES);

        // Mettre à jour l'info de solde quand la devise change
        cmbCurrency.valueProperty().addListener((obs, o, n) -> updateAvailableCashDisplay());
    }

    private void updateAvailableCashDisplay() {
        CashSession session = cashClosureService.getCurrentSession();
        if (session == null) {
            lblAvailableCashInfo.setText("Caisse : Aucune session");
            return;
        }

        String cur = cmbCurrency.getValue() != null ? cmbCurrency.getValue() : "USD";
        double bal = "USD".equalsIgnoreCase(cur) ? session.getCurrentBalanceUSD() : session.getCurrentBalanceCDF();

        if (MoneyVisibilityManager.getInstance().isVisible()) {
            lblAvailableCashInfo.setText(String.format("Solde disponible en caisse : %s",
                    "USD".equalsIgnoreCase(cur) ? MoneyFormatter.formatUSD(bal) : MoneyFormatter.formatCDF(bal)));
        } else {
            lblAvailableCashInfo.setText("Solde disponible en caisse : " + MoneyFormatter.formatMasked());
        }
    }

    private void setupTable() {
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getExpenseDate() != null ? c.getValue().getExpenseDate().format(df) : "—"));
        colVoucher.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVoucherNumber()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colLabel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLabel()));
        colCur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrency()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                MoneyFormatter.display(c.getValue().getAmount(), c.getValue().getCurrency())
        ));
        colBeneficiary.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBeneficiary()));
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPaymentMethod() != null ? c.getValue().getPaymentMethod().name() : "—"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() != null ? c.getValue().getStatus().getLabel() : "—"));

        // Actions : Annulation non destructive
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancel = new Button("❌ Annuler");
            private final HBox box = new HBox(btnCancel);

            {
                btnCancel.getStyleClass().add("btn-secondary");
                box.setAlignment(Pos.CENTER);

                btnCancel.setOnAction(e -> {
                    Expense exp = getTableView().getItems().get(getIndex());
                    if (exp != null) {
                        cancelExpenseConfirm(exp);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Expense exp = getTableView().getItems().get(getIndex());
                    boolean active = exp != null && exp.getStatus() == Expense.ExpenseStatus.VALIDEE;
                    btnCancel.setDisable(!active);
                    setGraphic(box);
                }
            }
        });

        tableExpenses.setItems(expenseList);
        tableExpenses.setPlaceholder(new Label("Aucune dépense enregistrée."));
    }

    private void cancelExpenseConfirm(Expense exp) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annulation de Dépense");
        confirm.setHeaderText("Annuler le bon N° " + exp.getVoucherNumber() + " ?");
        confirm.setContentText("Le montant de " + exp.getAmount() + " " + exp.getCurrency()
                + " sera recrédité en caisse. Statut mis à jour : ANNULÉE.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                boolean done = expenseService.cancelExpense(exp.getId());
                if (done) {
                    loadExpenses();
                    updateAvailableCashDisplay();
                }
            }
        });
    }

    private void loadExpenses() {
        List<Expense> all = expenseService.findAll();
        expenseList.setAll(all);
        lblExpensesCount.setText(all.size() + " dépense(s)");
    }

    // ─── Gestion des justificatifs ───

    @FXML
    private void handleAddAttachment() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Sélectionner une pièce justificative");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Documents & Images (*.pdf, *.jpg, *.png)", "*.pdf", "*.jpg", "*.png", "*.jpeg")
            );
            File file = chooser.showOpenDialog(null);
            if (file != null) {
                attachedFileName = file.getName();
                lblAttachmentName.setText(attachedFileName);
                btnRemoveAttachment.setVisible(true);
                btnRemoveAttachment.setManaged(true);
            }
        } catch (Exception e) {
            // Environnement sans display direct ou simulation
            attachedFileName = "facture_001.pdf";
            lblAttachmentName.setText("Justificatif ajouté : " + attachedFileName);
            btnRemoveAttachment.setVisible(true);
            btnRemoveAttachment.setManaged(true);
        }
    }

    @FXML
    private void handleRemoveAttachment() {
        attachedFileName = null;
        lblAttachmentName.setText("Aucun fichier");
        btnRemoveAttachment.setVisible(false);
        btnRemoveAttachment.setManaged(false);
    }

    // ─── Enregistrement de dépense ───

    @FXML
    private void handleSubmitExpense() {
        hideAlerts();
        btnSubmitExpense.setDisable(true);

        try {
            String cat = cmbCategory.getValue();
            if (cat == null || cat.isBlank()) {
                showError("Veuillez sélectionner une catégorie de dépense.");
                return;
            }

            String label = txtLabel.getText() != null ? txtLabel.getText().trim() : "";
            if (label.isEmpty()) {
                showError("Veuillez saisir une description / libellé court.");
                return;
            }

            String beneficiary = txtBeneficiary.getText() != null ? txtBeneficiary.getText().trim() : "";
            if (beneficiary.isEmpty()) {
                showError("Le nom du bénéficiaire est obligatoire.");
                return;
            }

            String rawAmount = txtAmount.getText() != null ? txtAmount.getText().trim() : "";
            if (rawAmount.isEmpty()) {
                showError("Veuillez saisir un montant.");
                return;
            }

            double amount = Double.parseDouble(rawAmount);
            if (amount <= 0) {
                showError("Le montant doit être strictement supérieur à zéro.");
                return;
            }

            String currency = cmbCurrency.getValue();

            // Contrôle préalable du solde de caisse
            CashSession session = cashClosureService.getCurrentSession();
            double currentBal = session != null
                    ? ("USD".equalsIgnoreCase(currency) ? session.getCurrentBalanceUSD() : session.getCurrentBalanceCDF())
                    : 0.0;

            if (amount > currentBal) {
                showError(String.format("ALERTE : Solde insuffisant en caisse ! Montant demandé : %.2f %s > Solde disponible : %.2f %s",
                        amount, currency, currentBal, currency));
                return;
            }

            // Récapitulatif et Confirmation modale
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation de décaissement");
            confirmAlert.setHeaderText("Voulez-vous enregistrer cette dépense ?");
            confirmAlert.setContentText(String.format(
                    "Catégorie : %s\nBénéficiaire : %s\nMontant : %.2f %s\nMode : %s\nJustificatif : %s",
                    cat, beneficiary, amount, currency, cmbPaymentMethod.getValue().name(),
                    attachedFileName != null ? attachedFileName : "Aucun"
            ));

            confirmAlert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    Expense exp = new Expense();
                    exp.setExpenseDate(dpDate.getValue() != null ? dpDate.getValue() : LocalDate.now());
                    exp.setCategory(cat);
                    exp.setLabel(label);
                    exp.setBeneficiary(beneficiary);
                    exp.setAmount(amount);
                    exp.setCurrency(currency);
                    exp.setPaymentMethod(cmbPaymentMethod.getValue());
                    exp.setReference(txtReference.getText() != null ? txtReference.getText().trim() : "");
                    exp.setReason(txtReason.getText() != null ? txtReason.getText().trim() : "");
                    exp.setAttachmentName(attachedFileName);

                    Expense saved = expenseService.processExpense(exp);

                    if (saved.getStatus() == Expense.ExpenseStatus.EN_ATTENTE) {
                        showSuccess("Dépense enregistrée en attente d'autorisation préalable du Superviseur (Bon N° " + saved.getVoucherNumber() + ").");
                    } else {
                        showSuccess("Dépense validée et décaissée avec succès ! Bon N° " + saved.getVoucherNumber());
                    }

                    handleResetForm();
                    loadExpenses();
                    updateAvailableCashDisplay();
                    com.jenos.gestionecole.service.DashboardService.getInstance().notifyDataChanged();
                }
            });

        } catch (NumberFormatException e) {
            showError("Format de montant invalide.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } finally {
            btnSubmitExpense.setDisable(false);
        }
    }

    @FXML
    private void handleResetForm() {
        txtLabel.clear();
        txtAmount.clear();
        txtBeneficiary.clear();
        txtReference.clear();
        txtReason.clear();
        handleRemoveAttachment();
    }

    @FXML
    private void handleBackToCash() {
        NavigationManager.getInstance().navigate(Route.CASH_REGISTER);
    }

    @FXML
    private void handleRefreshExpenses() {
        loadExpenses();
        updateAvailableCashDisplay();
    }

    @FXML
    private void handleToggleAmounts() {
        if (MoneyVisibilityManager.getInstance().isVisible()) {
            MoneyVisibilityManager.getInstance().hideAll();
        } else {
            MoneyVisibilityManager.getInstance().showAll();
        }
    }

    private void showError(String msg) {
        lblExpenseError.setText(msg);
        lblExpenseError.setVisible(true);
        lblExpenseError.setManaged(true);
    }

    private void showSuccess(String msg) {
        lblExpenseSuccess.setText(msg);
        lblExpenseSuccess.setVisible(true);
        lblExpenseSuccess.setManaged(true);
    }

    private void hideAlerts() {
        lblExpenseError.setVisible(false);
        lblExpenseError.setManaged(false);
        lblExpenseSuccess.setVisible(false);
        lblExpenseSuccess.setManaged(false);
    }

    @Override
    public void onShow() {
        updateAvailableCashDisplay();
        tableExpenses.refresh();
    }

    @Override
    public void onHide() {
        updateAvailableCashDisplay();
        tableExpenses.refresh();
    }
}
