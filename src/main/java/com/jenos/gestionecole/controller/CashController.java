package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.CashRegister;
import com.jenos.gestionecole.model.CashSession;
import com.jenos.gestionecole.service.CashClosureService;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

/**
 * CashController — Écran principal de gestion et consultation de caisse.
 */
public class CashController implements MoneyVisibilityManager.VisibilityObserver {

    @FXML private ComboBox<CashRegister> cmbRegisters;
    @FXML private HBox boxRegisterSelector;

    // ── Cartes État ──
    @FXML private Label lblCashStatus;
    @FXML private Label lblCashierName;
    @FXML private Label lblOpenTime;

    // ── Soldes ──
    @FXML private Label lblBalanceUSD;
    @FXML private Label lblBalanceCDF;
    @FXML private Label lblReportStatus;

    // ── Boutons d'action ──
    @FXML private Button btnDailyReport;
    @FXML private Button btnPrintReport;
    @FXML private Button btnCloseCash;

    private final CashClosureService closureService = CashClosureService.getInstance();
    private CashSession session;

    @FXML
    public void initialize() {
        MoneyVisibilityManager.getInstance().registerObserver(this);
        session = closureService.getCurrentSession();
        setupView();
    }

    private void setupView() {
        // Sélecteur de caisses pour promoteur
        boolean canSwitch = RoleGuard.canViewOtherCashRegisters();
        boxRegisterSelector.setVisible(canSwitch);
        boxRegisterSelector.setManaged(canSwitch);

        if (canSwitch) {
            cmbRegisters.setItems(FXCollections.observableArrayList(closureService.getAllRegisters()));
            if (session != null) {
                cmbRegisters.setValue(session.getCashRegister());
            }
        }

        refreshSessionInfo();
    }

    private void refreshSessionInfo() {
        if (session == null) return;

        lblCashStatus.setText(session.getCashRegister() != null ? session.getCashRegister().getStatus().name() : "INCONNU");
        lblCashierName.setText("Caissière : " + session.getCashierUsername());
        lblOpenTime.setText("Ouverte depuis : " + (session.getOpenedAt() != null ? session.getOpenedAt().toLocalTime().toString().substring(0, 5) : "—"));

        boolean repGen = session.isReportGenerated();
        boolean repPrt = session.isReportPrinted();
        lblReportStatus.setText(repGen && repPrt ? "Rapport Imprimé ✓" : (repGen ? "Rapport Généré (non imprimé)" : "Rapport Non Généré"));

        refreshMoneyDisplay();
    }

    private void refreshMoneyDisplay() {
        if (session == null) return;
        lblBalanceUSD.setText(MoneyFormatter.display(session.getCurrentBalanceUSD(), "USD"));
        lblBalanceCDF.setText(MoneyFormatter.display(session.getCurrentBalanceCDF(), "CDF"));
    }

    @Override
    public void onShow() {
        refreshMoneyDisplay();
    }

    @Override
    public void onHide() {
        refreshMoneyDisplay();
    }

    @FXML
    private void handleGenerateDailyReport() {
        closureService.generateDailyReport();
        refreshSessionInfo();
        NavigationManager.getInstance().navigate(Route.REPORTS);
    }

    @FXML
    private void handlePrintReport() {
        closureService.markDailyReportPrinted();
        refreshSessionInfo();
    }

    @FXML
    private void handleGoToClosure() {
        NavigationManager.getInstance().navigate(Route.CLOSE_CASH_DIALOG);
    }
}
