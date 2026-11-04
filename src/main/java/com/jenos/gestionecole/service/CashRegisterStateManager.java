package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CashRegisterStateManager — Composant central de gestion de l'état du cycle de caisse.
 *
 * RÈGLES ABSOLUES DU SYSTÈME :
 * 1. Les devises USD et CDF sont gérées séparément, JAMAIS additionnées ni converties.
 * 2. Le cycle de clôture normale est verrouillé :
 *    RAPPORT JOURNALIER -> GÉNÉRÉ -> IMPRIMÉ -> COMPTAGE PHYSIQUE -> RAPPROCHEMENT -> CONFIRMATION -> CLÔTURÉE.
 * 3. Aucune fermeture normale n'est autorisée si le rapport journalier n'a pas été généré ET imprimé.
 * 4. Une caisse fermée n'accepte plus d'opérations financières.
 */
public class CashRegisterStateManager {

    public enum CashRegisterState {
        FERMEE,
        OUVERTURE_EN_COURS,
        OUVERTE,
        RAPPORT_EN_ATTENTE,
        RAPPORT_GENERE,
        RAPPORT_IMPRIME,
        COMPTAGE_EN_COURS,
        ECART_DETECTE,
        CLOTURE_EN_COURS,
        CLOTUREE,
        ERREUR
    }

    public enum ReportStatus {
        NON_GENERE,
        GENERE,
        IMPRIME,
        ECHEC_GENERATION
    }

    public static class DailyReportData {
        private String reportNumber;
        private LocalDateTime generatedAt;
        private String cashierUsername;
        private String cashRegisterName;
        private LocalDate date;

        // Soldes théoriques CDF
        private double initialBalanceCDF;
        private double totalInCDF;
        private double totalOutCDF;
        private double theoreticalBalanceCDF;

        // Soldes théoriques USD
        private double initialBalanceUSD;
        private double totalInUSD;
        private double totalOutUSD;
        private double theoreticalBalanceUSD;

        // Détails des paiements & dépenses
        private int paymentCount;
        private int expenseCount;

        // Paiements par mode
        private double paymentsCashUSD;
        private double paymentsCashCDF;
        private double paymentsMobileMoneyUSD;
        private double paymentsMobileMoneyCDF;
        private double paymentsBankUSD;
        private double paymentsBankCDF;

        // Dépenses par catégorie
        private List<CategorySummary> expenseCategories = new ArrayList<>();

        public String getReportNumber() { return reportNumber; }
        public void setReportNumber(String reportNumber) { this.reportNumber = reportNumber; }

        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public String getCashierUsername() { return cashierUsername; }
        public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

        public String getCashRegisterName() { return cashRegisterName; }
        public void setCashRegisterName(String cashRegisterName) { this.cashRegisterName = cashRegisterName; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public double getInitialBalanceCDF() { return initialBalanceCDF; }
        public void setInitialBalanceCDF(double v) { this.initialBalanceCDF = v; }

        public double getTotalInCDF() { return totalInCDF; }
        public void setTotalInCDF(double v) { this.totalInCDF = v; }

        public double getTotalOutCDF() { return totalOutCDF; }
        public void setTotalOutCDF(double v) { this.totalOutCDF = v; }

        public double getTheoreticalBalanceCDF() { return theoreticalBalanceCDF; }
        public void setTheoreticalBalanceCDF(double v) { this.theoreticalBalanceCDF = v; }

        public double getInitialBalanceUSD() { return initialBalanceUSD; }
        public void setInitialBalanceUSD(double v) { this.initialBalanceUSD = v; }

        public double getTotalInUSD() { return totalInUSD; }
        public void setTotalInUSD(double v) { this.totalInUSD = v; }

        public double getTotalOutUSD() { return totalOutUSD; }
        public void setTotalOutUSD(double v) { this.totalOutUSD = v; }

        public double getTheoreticalBalanceUSD() { return theoreticalBalanceUSD; }
        public void setTheoreticalBalanceUSD(double v) { this.theoreticalBalanceUSD = v; }

        public int getPaymentCount() { return paymentCount; }
        public void setPaymentCount(int paymentCount) { this.paymentCount = paymentCount; }

        public int getExpenseCount() { return expenseCount; }
        public void setExpenseCount(int expenseCount) { this.expenseCount = expenseCount; }

        public double getPaymentsCashUSD() { return paymentsCashUSD; }
        public void setPaymentsCashUSD(double v) { this.paymentsCashUSD = v; }

        public double getPaymentsCashCDF() { return paymentsCashCDF; }
        public void setPaymentsCashCDF(double v) { this.paymentsCashCDF = v; }

        public double getPaymentsMobileMoneyUSD() { return paymentsMobileMoneyUSD; }
        public void setPaymentsMobileMoneyUSD(double v) { this.paymentsMobileMoneyUSD = v; }

        public double getPaymentsMobileMoneyCDF() { return paymentsMobileMoneyCDF; }
        public void setPaymentsMobileMoneyCDF(double v) { this.paymentsMobileMoneyCDF = v; }

        public double getPaymentsBankUSD() { return paymentsBankUSD; }
        public void setPaymentsBankUSD(double v) { this.paymentsBankUSD = v; }

        public double getPaymentsBankCDF() { return paymentsBankCDF; }
        public void setPaymentsBankCDF(double v) { this.paymentsBankCDF = v; }

        public List<CategorySummary> getExpenseCategories() { return expenseCategories; }
        public void setExpenseCategories(List<CategorySummary> c) { this.expenseCategories = c; }
    }

    public static class CategorySummary {
        private String category;
        private double totalUSD;
        private double totalCDF;

        public CategorySummary(String category, double totalUSD, double totalCDF) {
            this.category = category;
            this.totalUSD = totalUSD;
            this.totalCDF = totalCDF;
        }

        public String getCategory() { return category; }
        public double getTotalUSD() { return totalUSD; }
        public double getTotalCDF() { return totalCDF; }
    }

    private static CashRegisterStateManager instance;

    private CashRegisterState currentState = CashRegisterState.OUVERTE;
    private ReportStatus reportStatus = ReportStatus.NON_GENERE;
    private DailyReportData lastDailyReport;

    private double totalInUSD = 0.0;
    private double totalInCDF = 0.0;
    private double totalOutUSD = 0.0;
    private double totalOutCDF = 0.0;

    private String varianceReasonCDF = "";
    private String varianceReasonUSD = "";

    private CashRegisterStateManager() {
        initDefaultTotals();
    }

    public static CashRegisterStateManager getInstance() {
        if (instance == null) {
            instance = new CashRegisterStateManager();
        }
        return instance;
    }

    private void initDefaultTotals() {
        // Initialiser quelques opérations de démonstration
        totalInUSD = 150.0;
        totalInCDF = 85000.0;
        totalOutUSD = 65.0;
        totalOutCDF = 0.0;
    }

    // ─────────────────────────────────────────────────────────────
    // GESTION DE L'ÉTAT ET VÉRIFICATION DE CLÔTURE
    // ─────────────────────────────────────────────────────────────

    public CashRegisterState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(CashRegisterState state) {
        this.currentState = state;
    }

    public ReportStatus getReportStatus() {
        return reportStatus;
    }

    public boolean isCashOpen() {
        return currentState != CashRegisterState.FERMEE && currentState != CashRegisterState.CLOTUREE;
    }

    /**
     * Vérifie si la caisse est prête pour la clôture normale :
     * Le rapport doit être GÉNÉRÉ et IMPRIMÉ.
     */
    public boolean isReadyForNormalClosure() {
        return reportStatus == ReportStatus.IMPRIME;
    }

    public DailyReportData getLastDailyReport() {
        return lastDailyReport;
    }

    // ─────────────────────────────────────────────────────────────
    // GÉNÉRATION & IMPRESSION DU RAPPORT JOURNALIER
    // ─────────────────────────────────────────────────────────────

    public DailyReportData generateDailyReport() {
        CashSession session = CashClosureService.getInstance().getCurrentSession();
        DailyReportData report = new DailyReportData();

        int year = LocalDate.now().getYear();
        String dateStr = LocalDate.now().toString().replace("-", "");
        report.setReportNumber(String.format("RAP-%d-%s-001", year, dateStr.substring(4)));
        report.setGeneratedAt(LocalDateTime.now());
        report.setDate(LocalDate.now());

        if (session != null) {
            report.setCashierUsername(session.getCashierUsername());
            report.setCashRegisterName(session.getCashRegister() != null ? session.getCashRegister().getName() : "Caisse Principale 01");

            // CDF
            report.setInitialBalanceCDF(session.getInitialBalanceCDF());
            report.setTotalInCDF(totalInCDF);
            report.setTotalOutCDF(totalOutCDF);
            report.setTheoreticalBalanceCDF(session.getInitialBalanceCDF() + totalInCDF - totalOutCDF);

            // USD
            report.setInitialBalanceUSD(session.getInitialBalanceUSD());
            report.setTotalInUSD(totalInUSD);
            report.setTotalOutUSD(totalOutUSD);
            report.setTheoreticalBalanceUSD(session.getInitialBalanceUSD() + totalInUSD - totalOutUSD);
        }

        // Répartition
        report.setPaymentsCashUSD(totalInUSD);
        report.setPaymentsCashCDF(totalInCDF);
        report.setPaymentCount(2);
        report.setExpenseCount(1);

        List<CategorySummary> cats = new ArrayList<>();
        cats.add(new CategorySummary("Fournitures", totalOutUSD, 0.0));
        report.setExpenseCategories(cats);

        this.lastDailyReport = report;
        this.reportStatus = ReportStatus.GENERE;
        this.currentState = CashRegisterState.RAPPORT_GENERE;

        // Synchronisation avec CashClosureService
        CashClosureService.getInstance().generateDailyReport();

        return report;
    }

    public void markReportPrinted() {
        this.reportStatus = ReportStatus.IMPRIME;
        this.currentState = CashRegisterState.RAPPORT_IMPRIME;
        CashClosureService.getInstance().markDailyReportPrinted();
    }

    // ─────────────────────────────────────────────────────────────
    // RAPPROCHEMENT & CLÔTURE
    // ─────────────────────────────────────────────────────────────

    public void recordCounting(double countedCDF, double countedUSD, String reasonCDF, String reasonUSD) {
        CashSession session = CashClosureService.getInstance().getCurrentSession();
        if (session != null) {
            CashClosureService.getInstance().recordCounting(countedUSD, countedCDF);
        }
        this.varianceReasonCDF = reasonCDF;
        this.varianceReasonUSD = reasonUSD;

        double diffCDF = session != null ? (countedCDF - session.getCurrentBalanceCDF()) : 0;
        double diffUSD = session != null ? (countedUSD - session.getCurrentBalanceUSD()) : 0;

        if (Math.abs(diffCDF) > 0.01 || Math.abs(diffUSD) > 0.01) {
            this.currentState = CashRegisterState.ECART_DETECTE;
        } else {
            this.currentState = CashRegisterState.COMPTAGE_EN_COURS;
        }
    }

    public void confirmClosure(String notes) {
        CashSession session = CashClosureService.getInstance().getCurrentSession();
        if (session != null) {
            CashClosureService.getInstance().finalizeClosure(notes);
        }
        this.currentState = CashRegisterState.CLOTUREE;
    }

    public void forceClosure(String supervisorReason) {
        CashSession session = CashClosureService.getInstance().getCurrentSession();
        if (session != null) {
            CashClosureService.getInstance().forceClosure(supervisorReason, "Promoteur");
        }
        this.currentState = CashRegisterState.CLOTUREE;
    }

    public void openNewSession(double initUSD, double initCDF, String cashier) {
        CashSession session = CashClosureService.getInstance().getCurrentSession();
        if (session != null) {
            session.setInitialBalanceUSD(initUSD);
            session.setInitialBalanceCDF(initCDF);
            session.setCurrentBalanceUSD(initUSD);
            session.setCurrentBalanceCDF(initCDF);
            session.setCashierUsername(cashier);
            session.setOpenedAt(LocalDateTime.now());
            session.setClosedAt(null);
            session.setReportGenerated(false);
            session.setReportPrinted(false);
            session.setClosureState(CashSession.ClosureState.OPEN);
        }
        this.totalInUSD = 0.0;
        this.totalInCDF = 0.0;
        this.totalOutUSD = 0.0;
        this.totalOutCDF = 0.0;
        this.reportStatus = ReportStatus.NON_GENERE;
        this.lastDailyReport = null;
        this.currentState = CashRegisterState.OUVERTE;
    }

    // ─────────────────────────────────────────────────────────────
    // TOTAUX DU JOUR SÉPARÉS
    // ─────────────────────────────────────────────────────────────

    public double getTotalInUSD() { return totalInUSD; }
    public double getTotalInCDF() { return totalInCDF; }
    public double getTotalOutUSD() { return totalOutUSD; }
    public double getTotalOutCDF() { return totalOutCDF; }

    public void addIncome(double amount, String currency) {
        if ("USD".equalsIgnoreCase(currency)) {
            totalInUSD += amount;
        } else if ("CDF".equalsIgnoreCase(currency)) {
            totalInCDF += amount;
        }
    }

    public void addExpense(double amount, String currency) {
        if ("USD".equalsIgnoreCase(currency)) {
            totalOutUSD += amount;
        } else if ("CDF".equalsIgnoreCase(currency)) {
            totalOutCDF += amount;
        }
    }

    public String getVarianceReasonCDF() { return varianceReasonCDF; }
    public String getVarianceReasonUSD() { return varianceReasonUSD; }
}
