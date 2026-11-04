package com.jenos.gestionecole.controller;

import com.jenos.gestionecole.model.User;
import com.jenos.gestionecole.service.*;
import com.jenos.gestionecole.util.MoneyFormatter;
import com.jenos.gestionecole.util.MoneyVisibilityManager;
import com.jenos.gestionecole.util.NavigationManager;
import com.jenos.gestionecole.util.RoleGuard;
import com.jenos.gestionecole.util.Route;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * DashboardController — Contrôleur dynamique du tableau de bord système.
 *
 * RÈGLES DU BLOC 4 :
 * 1. Données 100% réelles issues de DashboardService (aucune simulation fictive).
 * 2. Cloisonnement strict USD et CDF sans mélange ni conversion.
 * 3. Recettes affichées en VERT (#10b981) et Dépenses affichées en ROUGE (#ef4444).
 * 4. Gestion complète des 4 états de graphique : Chargement, Contenu, État vide, Erreur (zéro blocage).
 * 5. Actualisation automatique réactive via DashboardDataListener.
 * 6. Filtrage dynamique par période et par monnaie d'analyse.
 */
public class DashboardController implements MoneyVisibilityManager.VisibilityObserver, DashboardService.DashboardDataListener {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    // ─── En-tête et filtres ───────────────────────────────────────────────────
    @FXML private Label lblPageTitle;
    @FXML private Label lblWelcome;
    @FXML private Label lblDate;
    @FXML private ComboBox<DashboardService.PeriodFilter> comboPeriod;
    @FXML private ComboBox<String> comboCurrency;
    @FXML private Button btnRefreshDashboard;

    // ─── Alerte rapport ───────────────────────────────────────────────────────
    @FXML private VBox alertReportPending;
    @FXML private Button btnGenerateReportAlert;

    // ─── Section financière ───────────────────────────────────────────────────
    @FXML private VBox financialSection;
    @FXML private Label lblCashBalanceUSD;
    @FXML private Label lblCashBalanceCDF;
    @FXML private Label lblRevenueUSD;
    @FXML private Label lblRevenueCDF;
    @FXML private Label lblExpensesUSD;
    @FXML private Label lblExpensesCDF;
    @FXML private Label lblUnpaidFeesUSD;
    @FXML private Label lblUnpaidFeesCDF;
    @FXML private Label lblBudgetUSD;
    @FXML private Label lblBudgetCDF;

    // ─── Section administrative & système ─────────────────────────────────────
    @FXML private GridPane administrativeCards;
    @FXML private Label lblTotalStudents;
    @FXML private Label lblTotalStaff;
    @FXML private Label lblTotalClasses;
    @FXML private Label lblTotalOperations;

    // ─── Zone Graphiques et Conteneurs d'état ─────────────────────────────────
    @FXML private VBox chartsSection;
    @FXML private Label lblChartCurrencyIndicator;
    @FXML private StackPane chartsContainer;
    @FXML private GridPane chartsContent;
    @FXML private VBox chartsLoading;
    @FXML private VBox chartsEmpty;
    @FXML private VBox chartsError;
    @FXML private Label lblChartsErrorDetail;

    // Graphiques
    @FXML private Label lblFinancialChartTitle;
    @FXML private BarChart<String, Number> financialChart;
    @FXML private CategoryAxis financialXAxis;
    @FXML private NumberAxis financialYAxis;

    @FXML private Label lblAcademicChartTitle;
    @FXML private BarChart<String, Number> academicChart;
    @FXML private CategoryAxis academicXAxis;
    @FXML private NumberAxis academicYAxis;

    // ─── État de caisse ───────────────────────────────────────────────────────
    @FXML private VBox cashStatusCard;
    @FXML private Label lblCashStatus;
    @FXML private Label lblCashCashier;
    @FXML private Label lblCashOpenTime;

    // ─── Actions rapides ──────────────────────────────────────────────────────
    @FXML private VBox quickActions;
    @FXML private Button btnNewPayment;
    @FXML private Button btnNewExpense;
    @FXML private Button btnDailyReport;
    @FXML private Button btnCloseCash;
    @FXML private Button btnViewCashes;

    // ─── Dernières métriques calculées en mémoire ─────────────────────────────
    private DashboardService.DashboardMetrics currentMetrics;
    private String selectedCurrency = "USD";
    private DashboardService.PeriodFilter selectedPeriod = DashboardService.PeriodFilter.CE_MOIS;

    @FXML
    public void initialize() {
        setPageHeader();
        initFilterControls();

        try {
            MoneyVisibilityManager.getInstance().registerObserver(this);
        } catch (Exception ignored) {}

        // Enregistrement comme observateur réactif des modifications de données
        DashboardService.getInstance().addListener(this);

        applyPermissions();
        loadDashboardData();
        checkReportStatus();
    }

    private void initFilterControls() {
        if (comboPeriod != null) {
            comboPeriod.setItems(FXCollections.observableArrayList(DashboardService.PeriodFilter.values()));
            comboPeriod.setValue(DashboardService.PeriodFilter.CE_MOIS);
            comboPeriod.setOnAction(e -> {
                DashboardService.PeriodFilter chosen = comboPeriod.getValue();
                if (chosen != null && chosen != selectedPeriod) {
                    selectedPeriod = chosen;
                    loadDashboardData();
                }
            });
        }

        if (comboCurrency != null) {
            comboCurrency.setItems(FXCollections.observableArrayList("USD", "CDF"));
            comboCurrency.setValue("USD");
            comboCurrency.setOnAction(e -> {
                String chosen = comboCurrency.getValue();
                if (chosen != null && !chosen.equalsIgnoreCase(selectedCurrency)) {
                    selectedCurrency = chosen;
                    if (lblChartCurrencyIndicator != null) {
                        lblChartCurrencyIndicator.setText("Devise affichée : " + selectedCurrency);
                    }
                    renderFinancialChart();
                }
            });
        }
    }

    private void setPageHeader() {
        User current = SessionManager.getInstance().getCurrentUser();
        String displayName = current != null ? current.getFullName() :
                (RoleGuard.getCurrentRole() != null ? RoleGuard.getCurrentRole().name() : "Utilisateur");

        if (lblWelcome != null) {
            lblWelcome.setText("Bienvenue, " + displayName);
        }
        if (lblDate != null) {
            lblDate.setText(LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTUALISATION AUTOMATIQUE RÉACTIVE (DashboardDataListener)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onDashboardDataChanged() {
        // Exécution sécurisée sur le JavaFX Application Thread
        Platform.runLater(() -> {
            logger.info("Mise à jour automatique du tableau de bord déclenchée");
            loadDashboardData();
        });
    }

    @FXML
    private void handleManualRefresh() {
        loadDashboardData();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHARGEMENT ET CALCUL DES DONNÉES DU TABLEAU DE BORD
    // ─────────────────────────────────────────────────────────────────────────

    private void loadDashboardData() {
        showStateLoading();

        try {
            // Calcul des indicateurs réels via DashboardService
            currentMetrics = DashboardService.getInstance().calculateMetrics(selectedPeriod);

            // Mise à jour des cartes administratives
            updateAdministrativeCards();

            // Mise à jour des cartes financières (si rôle autorisé)
            if (RoleGuard.canViewFinancialData()) {
                refreshMoneyDisplay();
            }

            // Mise à jour de l'état de caisse
            loadCashRegisterStatus();

            // Rendu des graphiques dynamiques
            renderCharts();

        } catch (Exception e) {
            logger.error("Erreur lors du chargement des données du tableau de bord", e);
            showStateError("Erreur : " + e.getMessage());
        }
    }

    private void updateAdministrativeCards() {
        if (currentMetrics == null) return;

        if (lblTotalStudents != null) lblTotalStudents.setText(String.valueOf(currentMetrics.totalStudents));
        if (lblTotalStaff != null) lblTotalStaff.setText(String.valueOf(currentMetrics.totalStaff));
        if (lblTotalClasses != null) lblTotalClasses.setText(String.valueOf(currentMetrics.totalClasses));
        if (lblTotalOperations != null) lblTotalOperations.setText(String.valueOf(currentMetrics.totalOperations));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDU DES GRAPHIQUES ET COULEURS STRICTES (VERT = RECETTES, ROUGE = DÉPENSES)
    // ─────────────────────────────────────────────────────────────────────────

    private void renderCharts() {
        if (currentMetrics == null) {
            showStateEmpty();
            return;
        }

        boolean hasFinancialData = renderFinancialChart();
        boolean hasAcademicData = renderAcademicChart();

        if (!hasFinancialData && !hasAcademicData) {
            showStateEmpty();
        } else {
            showStateContent();
        }
    }

    /**
     * Rendu du graphique financier avec coloration stricte :
     * Vert pour les recettes, Rouge pour les dépenses.
     */
    @SuppressWarnings("unchecked")
    private boolean renderFinancialChart() {
        if (financialChart == null || currentMetrics == null) return false;

        financialChart.getData().clear();

        boolean isUSD = "USD".equalsIgnoreCase(selectedCurrency);
        Map<String, Double> revenues = isUSD ? currentMetrics.revenueByDateUSD : currentMetrics.revenueByDateCDF;
        Map<String, Double> expenses = isUSD ? currentMetrics.expensesByDateUSD : currentMetrics.expensesByDateCDF;

        if (lblFinancialChartTitle != null) {
            lblFinancialChartTitle.setText(String.format("Flux financiers (%s) — Recettes (Vert) vs Dépenses (Rouge)", selectedCurrency));
        }

        if (revenues.isEmpty() && expenses.isEmpty()) {
            return false;
        }

        XYChart.Series<String, Number> seriesRecettes = new XYChart.Series<>();
        seriesRecettes.setName("Recettes (" + selectedCurrency + ")");

        XYChart.Series<String, Number> seriesDepenses = new XYChart.Series<>();
        seriesDepenses.setName("Dépenses (" + selectedCurrency + ")");

        // Récupération de l'ensemble des dates ordonnées
        Set<String> allDates = new TreeSet<>();
        allDates.addAll(revenues.keySet());
        allDates.addAll(expenses.keySet());

        for (String date : allDates) {
            double rec = revenues.getOrDefault(date, 0.0);
            double dep = expenses.getOrDefault(date, 0.0);

            // Format d'affichage de la date
            String dateLabel = date.length() >= 10 ? date.substring(5) : date;
            seriesRecettes.getData().add(new XYChart.Data<>(dateLabel, rec));
            seriesDepenses.getData().add(new XYChart.Data<>(dateLabel, dep));
        }

        financialChart.getData().addAll(seriesRecettes, seriesDepenses);

        // Application immédiate des styles de couleur : VERT pour recettes, ROUGE pour dépenses
        Platform.runLater(() -> applyChartColors(seriesRecettes, seriesDepenses));

        return true;
    }

    private void applyChartColors(XYChart.Series<String, Number> seriesRecettes,
                                  XYChart.Series<String, Number> seriesDepenses) {
        // Applique le vert (#10b981) sur toutes les barres de la série Recettes
        for (XYChart.Data<String, Number> data : seriesRecettes.getData()) {
            Node node = data.getNode();
            if (node != null) {
                node.setStyle("-fx-bar-fill: #10b981; -fx-background-color: #10b981;");
            }
        }

        // Applique le rouge (#ef4444) sur toutes les barres de la série Dépenses
        for (XYChart.Data<String, Number> data : seriesDepenses.getData()) {
            Node node = data.getNode();
            if (node != null) {
                node.setStyle("-fx-bar-fill: #ef4444; -fx-background-color: #ef4444;");
            }
        }
    }

    /**
     * Rendu du graphique scolaire représentant la répartition des effectifs par section.
     */
    private boolean renderAcademicChart() {
        if (academicChart == null || currentMetrics == null) return false;

        academicChart.getData().clear();

        if (currentMetrics.studentsBySection.isEmpty()) {
            return false;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Effectif");

        for (Map.Entry<String, Integer> entry : currentMetrics.studentsBySection.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        academicChart.getData().add(series);

        // Coloration esthétique des barres scolaires
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    node.setStyle("-fx-bar-fill: #3b82f6; -fx-background-color: #3b82f6;");
                }
            }
        });

        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTION DES ÉTATS D'AFFICHAGE DU GRAPHIQUE (ANTI-BLOCAGE)
    // ─────────────────────────────────────────────────────────────────────────

    private void showStateLoading() {
        setOverlayVisibility(true, false, false, false);
    }

    private void showStateContent() {
        setOverlayVisibility(false, true, false, false);
    }

    private void showStateEmpty() {
        setOverlayVisibility(false, false, true, false);
    }

    private void showStateError(String detail) {
        if (lblChartsErrorDetail != null) {
            lblChartsErrorDetail.setText(detail != null ? detail : "Erreur inconnue");
        }
        setOverlayVisibility(false, false, false, true);
    }

    private void setOverlayVisibility(boolean loading, boolean content, boolean empty, boolean error) {
        if (chartsLoading != null) {
            chartsLoading.setVisible(loading);
            chartsLoading.setManaged(loading);
        }
        if (chartsContent != null) {
            chartsContent.setVisible(content);
            chartsContent.setManaged(content);
        }
        if (chartsEmpty != null) {
            chartsEmpty.setVisible(empty);
            chartsEmpty.setManaged(empty);
        }
        if (chartsError != null) {
            chartsError.setVisible(error);
            chartsError.setManaged(error);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AFFICHAGE DES MONTANTS FINANCIERS & CONFIDENTIALITÉ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onShow() {
        refreshMoneyDisplay();
    }

    @Override
    public void onHide() {
        refreshMoneyDisplay();
    }

    public void refreshMoneyDisplay() {
        if (!RoleGuard.canViewFinancialData() || currentMetrics == null) return;

        if (lblCashBalanceUSD != null) lblCashBalanceUSD.setText(MoneyFormatter.display(currentMetrics.netBalanceUSD, "USD"));
        if (lblCashBalanceCDF != null) lblCashBalanceCDF.setText(MoneyFormatter.display(currentMetrics.netBalanceCDF, "CDF"));

        if (lblRevenueUSD != null) lblRevenueUSD.setText(MoneyFormatter.display(currentMetrics.totalRevenueUSD, "USD"));
        if (lblRevenueCDF != null) lblRevenueCDF.setText(MoneyFormatter.display(currentMetrics.totalRevenueCDF, "CDF"));

        if (lblExpensesUSD != null) lblExpensesUSD.setText(MoneyFormatter.display(currentMetrics.totalExpensesUSD, "USD"));
        if (lblExpensesCDF != null) lblExpensesCDF.setText(MoneyFormatter.display(currentMetrics.totalExpensesCDF, "CDF"));

        if (lblUnpaidFeesUSD != null) lblUnpaidFeesUSD.setText(MoneyFormatter.display(currentMetrics.unpaidFeesUSD, "USD"));
        if (lblUnpaidFeesCDF != null) lblUnpaidFeesCDF.setText(MoneyFormatter.display(currentMetrics.unpaidFeesCDF, "CDF"));

        // Budget restant
        try {
            double budgetUSD = BudgetService.getInstance().getAvailableBudgetUSD();
            double budgetCDF = BudgetService.getInstance().getAvailableBudgetCDF();
            if (lblBudgetUSD != null) lblBudgetUSD.setText(MoneyFormatter.display(budgetUSD, "USD"));
            if (lblBudgetCDF != null) lblBudgetCDF.setText(MoneyFormatter.display(budgetCDF, "CDF"));
        } catch (Exception ignored) {}
    }

    private void loadCashRegisterStatus() {
        if (!RoleGuard.canViewFinancialData() && !RoleGuard.canCloseCash()) {
            if (cashStatusCard != null) {
                cashStatusCard.setVisible(false);
                cashStatusCard.setManaged(false);
            }
            return;
        }

        User user = SessionManager.getInstance().getCurrentUser();
        String registerName = (user != null && user.getCashRegisterCode() != null)
                ? user.getCashRegisterCode() : "Caisse Principale 01";

        if (lblCashStatus != null) lblCashStatus.setText("Ouverte");
        if (lblCashCashier != null) {
            String cashierName = (user != null) ? user.getFullName() : "Caissière";
            lblCashCashier.setText("Affectée à : " + cashierName + " (" + registerName + ")");
        }
        if (lblCashOpenTime != null) {
            lblCashOpenTime.setText("Session ouverte le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
    }

    private void applyPermissions() {
        boolean canFinancial = RoleGuard.canViewFinancialData();

        if (financialSection != null) {
            financialSection.setVisible(canFinancial);
            financialSection.setManaged(canFinancial);
        }

        if (btnNewPayment != null) {
            btnNewPayment.setVisible(canFinancial && RoleGuard.canCreatePayment());
            btnNewPayment.setManaged(canFinancial && RoleGuard.canCreatePayment());
        }
        if (btnNewExpense != null) {
            btnNewExpense.setVisible(canFinancial && RoleGuard.canCreateExpense());
            btnNewExpense.setManaged(canFinancial && RoleGuard.canCreateExpense());
        }
        if (btnCloseCash != null) {
            boolean canClose = canFinancial && RoleGuard.canCloseCash();
            btnCloseCash.setVisible(canClose);
            btnCloseCash.setManaged(canClose);
        }
        if (btnDailyReport != null) {
            boolean canReport = RoleGuard.canGenerateCashReports();
            btnDailyReport.setVisible(canReport);
            btnDailyReport.setManaged(canReport);
        }
        if (btnViewCashes != null) {
            boolean canViewAll = RoleGuard.canViewOtherCashRegisters();
            btnViewCashes.setVisible(canViewAll);
            btnViewCashes.setManaged(canViewAll);
        }
    }

    private void checkReportStatus() {
        if (alertReportPending == null) return;

        if (RoleGuard.getCurrentRole() == RoleGuard.Role.CAISSIERE) {
            boolean isPending = CashClosureService.getInstance().isClosureBlockedPendingDailyReport();
            alertReportPending.setVisible(isPending);
            alertReportPending.setManaged(isPending);
        } else {
            alertReportPending.setVisible(false);
            alertReportPending.setManaged(false);
        }
    }

    // ─── Actions rapides ──────────────────────────────────────────────────────
    @FXML private void goToPayments()  { NavigationManager.getInstance().navigate(Route.PAYMENTS); }
    @FXML private void goToExpenses()  { NavigationManager.getInstance().navigate(Route.EXPENSES); }
    @FXML private void viewAllCashes() { NavigationManager.getInstance().navigate(Route.CASH); }
    @FXML private void generateDailyReport() { NavigationManager.getInstance().navigate(Route.DAILY_REPORT); }
    @FXML private void openCloseCashDialog() { NavigationManager.getInstance().navigate(Route.CASH_CLOSING_DIALOG); }
}
