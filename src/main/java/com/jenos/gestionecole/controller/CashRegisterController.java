package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.CashRegister;
import com.jenos.gestionecole.model.CashSession;
import com.jenos.gestionecole.model.Operation;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.service.CashRegisterStateManager;
import com.jenos.gestionecole.service.HistoryService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CashRegisterController — Contrôleur principal de l'écran de caisse enregistreuse.
 *
 * RÈGLES MÉTIER :
 * 1. Soldes et flux USD / CDF strictement séparés (jamais additionnés)
 * 2. Masquage dynamique via MoneyVisibilityManager
 * 3. Fermeture normale STRICTEMENT BLOQUÉE si rapport journalier non généré et non imprimé
 * 4. Désactivation des encaissements/décaissements si caisse fermée
 */
public class CashRegisterController implements MoneyVisibilityManager.VisibilityObserver {

    // ── En-tête ──
    @FXML private Label lblRegisterTitle;
    @FXML private Label lblCashStatus;
    @FXML private Label lblCashierName;
    @FXML private Label lblOpenTime;
    @FXML private Label lblReportIndicator;
    @FXML private HBox boxRegisterSelector;
    @FXML private ComboBox<CashRegister> cmbRegisters;
    @FXML private Button btnToggleAmounts;

    // ── Alerte caisse fermée ──
    @FXML private VBox boxClosedWarning;
    @FXML private Button btnOpenNewSession;

    // ── Cartes financières ──
    @FXML private GridPane cashSummary;
    @FXML private Label lblBalanceCDF;
    @FXML private Label lblBalanceUSD;
    @FXML private Label lblIncomeUSD;
    @FXML private Label lblIncomeCDF;
    @FXML private Label lblExpenseUSD;
    @FXML private Label lblExpenseCDF;

    // ── Actions rapides ──
    @FXML private HBox quickActions;
    @FXML private Button btnNewPayment;
    @FXML private Button btnNewExpense;
    @FXML private Button btnDailyReport;
    @FXML private Button btnCloseCash;

    // ── Tableau des opérations ──
    @FXML private Label lblOperationsCount;
    @FXML private Button btnRefreshOperations;
    @FXML private TableView<Operation> tblOperations;
    @FXML private TableColumn<Operation, String> colTime;
    @FXML private TableColumn<Operation, String> colRef;
    @FXML private TableColumn<Operation, String> colType;
    @FXML private TableColumn<Operation, String> colDesc;
    @FXML private TableColumn<Operation, String> colMethod;
    @FXML private TableColumn<Operation, String> colCur;
    @FXML private TableColumn<Operation, String> colAmount;
    @FXML private TableColumn<Operation, String> colStatus;

    private final CashClosureService closureService = CashClosureService.getInstance();
    private final CashRegisterStateManager stateManager = CashRegisterStateManager.getInstance();
    private final HistoryService historyService = HistoryService.getInstance();

    private final ObservableList<Operation> operationsList = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        MoneyVisibilityManager.getInstance().registerObserver(this);

        setupRegistersSelector();
        setupOperationsTable();
        refreshAll();
    }

    private void setupRegistersSelector() {
        boolean canViewAll = RoleGuard.canViewAllCashRegisters();
        boxRegisterSelector.setVisible(canViewAll);
        boxRegisterSelector.setManaged(canViewAll);

        if (canViewAll) {
            cmbRegisters.setItems(FXCollections.observableArrayList(closureService.getAllRegisters()));
            CashSession session = closureService.getCurrentSession();
            if (session != null) {
                cmbRegisters.setValue(session.getCashRegister());
            }
        }
    }

    private void setupOperationsTable() {
        colTime.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateTime() != null ? c.getValue().getDateTime().format(timeFormatter) : "—"));
        colRef.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReferenceNumber()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getType() != null ? c.getValue().getType().name() : "—"));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMethod()));
        colCur.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrency()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                MoneyFormatter.display(c.getValue().getAmount(), c.getValue().getCurrency())
        ));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() != null ? c.getValue().getStatus().name() : "—"));

        tblOperations.setItems(operationsList);
    }

    public void refreshAll() {
        CashSession session = closureService.getCurrentSession();
        if (session == null) return;

        // Statut & Titres
        boolean isOpen = stateManager.isCashOpen();
        lblRegisterTitle.setText(session.getCashRegister() != null ? session.getCashRegister().getName() : "Caisse 01");
        lblCashStatus.setText(isOpen ? "OUVERTE" : "FERMÉE");
        lblCashStatus.setStyle(isOpen
                ? "-fx-background-color: #d1e7dd; -fx-text-fill: #0f5132; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-font-weight: bold;"
                : "-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-font-weight: bold;"
        );

        lblCashierName.setText("Caissière : " + session.getCashierUsername());
        lblOpenTime.setText("Ouverture : " + (session.getOpenedAt() != null ? session.getOpenedAt().toLocalTime().toString().substring(0, 5) : "—"));

        // Indicateur rapport
        CashRegisterStateManager.ReportStatus repStat = stateManager.getReportStatus();
        switch (repStat) {
            case IMPRIME:
                lblReportIndicator.setText("Rapport : Imprimé ✓");
                lblReportIndicator.setStyle("-fx-text-fill: #198754; -fx-font-weight: bold;");
                break;
            case GENERE:
                lblReportIndicator.setText("Rapport : Généré (non imprimé) ⚠️");
                lblReportIndicator.setStyle("-fx-text-fill: #0d6efd; -fx-font-weight: bold;");
                break;
            default:
                lblReportIndicator.setText("Rapport : Non généré ✗");
                lblReportIndicator.setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                break;
        }

        // Caisse fermée ?
        boxClosedWarning.setVisible(!isOpen);
        boxClosedWarning.setManaged(!isOpen);
        btnNewPayment.setDisable(!isOpen);
        btnNewExpense.setDisable(!isOpen);

        refreshFinancialCards();
        loadTodayOperations();
    }

    private void refreshFinancialCards() {
        CashSession session = closureService.getCurrentSession();
        if (session == null) return;

        if (MoneyVisibilityManager.getInstance().isVisible()) {
            lblBalanceUSD.setText(String.format("%.2f USD", session.getCurrentBalanceUSD()));
            lblBalanceCDF.setText(String.format("%,.0f CDF", session.getCurrentBalanceCDF()));
            lblIncomeUSD.setText(String.format("USD : %.2f USD", stateManager.getTotalInUSD()));
            lblIncomeCDF.setText(String.format("CDF : %,.0f CDF", stateManager.getTotalInCDF()));
            lblExpenseUSD.setText(String.format("USD : %.2f USD", stateManager.getTotalOutUSD()));
            lblExpenseCDF.setText(String.format("CDF : %,.0f CDF", stateManager.getTotalOutCDF()));
        } else {
            lblBalanceUSD.setText(MoneyFormatter.formatMasked());
            lblBalanceCDF.setText(MoneyFormatter.formatMasked());
            lblIncomeUSD.setText("USD : " + MoneyFormatter.formatMasked());
            lblIncomeCDF.setText("CDF : " + MoneyFormatter.formatMasked());
            lblExpenseUSD.setText("USD : " + MoneyFormatter.formatMasked());
            lblExpenseCDF.setText("CDF : " + MoneyFormatter.formatMasked());
        }
    }

    private void loadTodayOperations() {
        CashSession session = closureService.getCurrentSession();
        String regName = session != null && session.getCashRegister() != null ? session.getCashRegister().getName() : null;

        List<Operation> todayOps = historyService.search(
                LocalDate.now(),
                LocalDate.now(),
                null,
                null,
                null,
                regName,
                null
        );

        operationsList.setAll(todayOps);
        lblOperationsCount.setText(todayOps.size() + " opération(s) aujourd'hui");
        tblOperations.refresh();
    }

    // ─── Actions rapides ───

    @FXML
    private void handleNewPayment() {
        if (!stateManager.isCashOpen()) {
            showClosedCashAlert();
            return;
        }
        NavigationManager.getInstance().navigate(Route.PAYMENTS);
    }

    @FXML
    private void handleNewExpense() {
        if (!stateManager.isCashOpen()) {
            showClosedCashAlert();
            return;
        }
        NavigationManager.getInstance().navigate(Route.EXPENSES);
    }

    @FXML
    private void handleDailyReport() {
        NavigationManager.getInstance().navigate(Route.DAILY_REPORT);
    }

    @FXML
    private void handleCloseCash() {
        // RÈGLE CENTRALE NON NÉGOCIABLE :
        // Le rapport journalier DOIT être généré ET imprimé pour la clôture normale !
        if (!stateManager.isReadyForNormalClosure()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("RAPPORT JOURNALIER MANQUANT");
            alert.setHeaderText("Action bloquée : Rapport journalier obligatoire");
            alert.setContentText("Vous devez obligatoirement générer et imprimer le rapport journalier de caisse avant de pouvoir procéder à la fermeture de la caisse.\n\nSouhaitez-vous générer le rapport maintenant ?");

            ButtonType btnGenerer = new ButtonType("📊 Générer le rapport", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnGenerer, btnAnnuler);

            alert.showAndWait().ifPresent(res -> {
                if (res == btnGenerer) {
                    NavigationManager.getInstance().navigate(Route.DAILY_REPORT);
                }
            });
            return;
        }

        // Si le rapport est bien généré et imprimé, ouvrir le dialogue de clôture
        NavigationManager.getInstance().navigate(Route.CASH_CLOSING_DIALOG);
    }

    @FXML
    private void handleToggleAmounts() {
        if (MoneyVisibilityManager.getInstance().isVisible()) {
            MoneyVisibilityManager.getInstance().hideAll();
        } else {
            MoneyVisibilityManager.getInstance().showAll();
        }
    }

    @FXML
    private void handleRefreshOperations() {
        refreshAll();
    }

    @FXML
    private void handleOpenNewSession() {
        TextInputDialog dialog = new TextInputDialog("50");
        dialog.setTitle("Ouverture de caisse");
        dialog.setHeaderText("Nouvelle Session de Caisse");
        dialog.setContentText("Solde initial USD :");

        dialog.showAndWait().ifPresent(valUSD -> {
            try {
                double initUSD = Double.parseDouble(valUSD.trim());
                stateManager.openNewSession(initUSD, 100000.0, "Jenos");
                refreshAll();
            } catch (NumberFormatException e) {
                // Ignore or show alert
            }
        });
    }

    private void showClosedCashAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Caisse fermée");
        alert.setHeaderText("Opération impossible");
        alert.setContentText("La caisse est actuellement fermée. Veuillez ouvrir une nouvelle session de caisse.");
        alert.showAndWait();
    }

    @Override
    public void onShow() {
        refreshFinancialCards();
        tblOperations.refresh();
    }

    @Override
    public void onHide() {
        refreshFinancialCards();
        tblOperations.refresh();
    }
}
