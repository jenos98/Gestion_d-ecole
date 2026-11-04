package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.*;
import com.jenos.gestionecole.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DashboardService — Service central de calcul des indicateurs et statistiques du tableau de bord.
 *
 * RÈGLES DU BLOC 4 :
 * 1. Données réelles uniquement (pas de valeurs fictives ou simulées).
 * 2. Cloisonnement strict USD et CDF sans aucune conversion automatique.
 * 3. Filtrage dynamique par période (Aujourd'hui, Cette semaine, Ce mois, Année scolaire, Tout).
 * 4. Calcul précis des recettes, dépenses, soldes, frais impayés et effectifs scolaires.
 * 5. Notification automatique et réactive des changements via le pattern Observer.
 */
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static DashboardService instance;

    // ── Interface de notification réactive ─────────────────────────────────────
    @FunctionalInterface
    public interface DashboardDataListener {
        void onDashboardDataChanged();
    }

    private final List<DashboardDataListener> listeners = new CopyOnWriteArrayList<>();

    // ── Filtres de période ─────────────────────────────────────────────────────
    public enum PeriodFilter {
        AUJOURDHUI("Aujourd'hui"),
        CETTE_SEMAINE("Cette semaine"),
        CE_MOIS("Ce mois"),
        CETTE_ANNEE("Année scolaire"),
        TOUT("Tout l'historique");

        private final String label;

        PeriodFilter(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ── Structure complète des métriques calculées ─────────────────────────────
    public static class DashboardMetrics {
        public PeriodFilter period;
        public LocalDate startDate;
        public LocalDate endDate;

        // Financier - Recettes réelles (paiements validés)
        public double totalRevenueUSD = 0.0;
        public double totalRevenueCDF = 0.0;

        // Financier - Dépenses réelles (sorties validées)
        public double totalExpensesUSD = 0.0;
        public double totalExpensesCDF = 0.0;

        // Financier - Solde net (Recettes - Dépenses)
        public double netBalanceUSD = 0.0;
        public double netBalanceCDF = 0.0;

        // Financier - Frais scolaires impayés (restant exigible après réductions)
        public double unpaidFeesUSD = 0.0;
        public double unpaidFeesCDF = 0.0;

        // Scolaire & Administratif
        public int totalStudents = 0;
        public int totalStaff = 0;
        public int totalClasses = 0;
        public int totalOperations = 0;

        // Séries pour graphique financier (catégorie/date -> montant)
        public final Map<String, Double> revenueByDateUSD = new LinkedHashMap<>();
        public final Map<String, Double> expensesByDateUSD = new LinkedHashMap<>();
        public final Map<String, Double> revenueByDateCDF = new LinkedHashMap<>();
        public final Map<String, Double> expensesByDateCDF = new LinkedHashMap<>();

        // Séries pour graphique scolaire (nom de section / classe -> effectif)
        public final Map<String, Integer> studentsBySection = new LinkedHashMap<>();
        public final Map<String, Integer> studentsByClass = new LinkedHashMap<>();
    }

    private DashboardService() {}

    public static synchronized DashboardService getInstance() {
        if (instance == null) {
            instance = new DashboardService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    // ── Gestion des observateurs ───────────────────────────────────────────────
    public void addListener(DashboardDataListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(DashboardDataListener listener) {
        listeners.remove(listener);
    }

    /**
     * Déclenche la notification réactive de mise à jour des observateurs.
     * Doit être appelé lors de chaque opération financière ou administrative réussie.
     */
    public void notifyDataChanged() {
        logger.info("Notification de modification de données reçue — réactualisation du tableau de bord");
        for (DashboardDataListener listener : listeners) {
            try {
                listener.onDashboardDataChanged();
            } catch (Exception e) {
                logger.error("Erreur lors de la notification d'un observateur du dashboard", e);
            }
        }
    }

    // ── Calcul des métriques réelles ───────────────────────────────────────────
    public DashboardMetrics calculateMetrics(PeriodFilter filter) {
        if (filter == null) filter = PeriodFilter.TOUT;

        DashboardMetrics metrics = new DashboardMetrics();
        metrics.period = filter;

        LocalDate now = LocalDate.now();
        switch (filter) {
            case AUJOURDHUI -> {
                metrics.startDate = now;
                metrics.endDate = now;
            }
            case CETTE_SEMAINE -> {
                metrics.startDate = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                metrics.endDate = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            }
            case CE_MOIS -> {
                metrics.startDate = now.withDayOfMonth(1);
                metrics.endDate = now.with(TemporalAdjusters.lastDayOfMonth());
            }
            case CETTE_ANNEE -> {
                int startYear = (now.getMonthValue() >= 9) ? now.getYear() : now.getYear() - 1;
                metrics.startDate = LocalDate.of(startYear, 9, 1);
                metrics.endDate = LocalDate.of(startYear + 1, 6, 30);
            }
            case TOUT -> {
                metrics.startDate = LocalDate.of(2000, 1, 1);
                metrics.endDate = LocalDate.of(2100, 12, 31);
            }
        }

        // 1. Recettes réelles (Paiements encaissés)
        computeRevenues(metrics);

        // 2. Dépenses réelles (Sorties de caisse)
        computeExpenses(metrics);

        // 3. Soldes nets (Recettes - Dépenses, strictement par devise)
        metrics.netBalanceUSD = metrics.totalRevenueUSD - metrics.totalExpensesUSD;
        metrics.netBalanceCDF = metrics.totalRevenueCDF - metrics.totalExpensesCDF;

        // 4. Frais impayés réels (Soldes des affectations de frais de scolarité)
        computeUnpaidFees(metrics);

        // 5. Statistiques scolaires et effectifs réels
        computeAcademicStats(metrics);

        return metrics;
    }

    private void computeRevenues(DashboardMetrics metrics) {
        try {
            List<Payment> payments = PaymentService.getInstance().findAllPayments();
            for (Payment p : payments) {
                if (p.getStatus() == Payment.PaymentStatus.VALIDEE) {
                    LocalDate date = p.getPaymentDate() != null ? p.getPaymentDate() :
                            (p.getCreatedAt() != null ? p.getCreatedAt().toLocalDate() : LocalDate.now());

                    if (!date.isBefore(metrics.startDate) && !date.isAfter(metrics.endDate)) {
                        String dateKey = date.toString();
                        if ("USD".equalsIgnoreCase(p.getCurrency())) {
                            metrics.totalRevenueUSD += p.getAmount();
                            metrics.revenueByDateUSD.merge(dateKey, p.getAmount(), Double::sum);
                        } else if ("CDF".equalsIgnoreCase(p.getCurrency())) {
                            metrics.totalRevenueCDF += p.getAmount();
                            metrics.revenueByDateCDF.merge(dateKey, p.getAmount(), Double::sum);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors du calcul des recettes", e);
        }
    }

    private void computeExpenses(DashboardMetrics metrics) {
        try {
            List<Operation> operations = HistoryService.getInstance().getAllOperations();
            for (Operation op : operations) {
                if (op.getType() == Operation.OperationType.DEPENSE
                        && op.getStatus() == Operation.OperationStatus.VALIDEE) {
                    LocalDate date = op.getDateTime() != null ? op.getDateTime().toLocalDate() : LocalDate.now();

                    if (!date.isBefore(metrics.startDate) && !date.isAfter(metrics.endDate)) {
                        String dateKey = date.toString();
                        if ("USD".equalsIgnoreCase(op.getCurrency())) {
                            metrics.totalExpensesUSD += op.getAmount();
                            metrics.expensesByDateUSD.merge(dateKey, op.getAmount(), Double::sum);
                        } else if ("CDF".equalsIgnoreCase(op.getCurrency())) {
                            metrics.totalExpensesCDF += op.getAmount();
                            metrics.expensesByDateCDF.merge(dateKey, op.getAmount(), Double::sum);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors du calcul des dépenses", e);
        }
    }

    private void computeUnpaidFees(DashboardMetrics metrics) {
        try {
            List<StudentFeeAssignment> assignments = SchoolFeeService.getInstance().getAllAssignments();
            for (StudentFeeAssignment a : assignments) {
                if (a.getStatus() == StudentFeeAssignment.AssignmentStatus.ACTIF) {
                    double balance = a.getBalance();
                    if (balance > 0) {
                        if ("USD".equalsIgnoreCase(a.getCurrency())) {
                            metrics.unpaidFeesUSD += balance;
                        } else if ("CDF".equalsIgnoreCase(a.getCurrency())) {
                            metrics.unpaidFeesCDF += balance;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors du calcul des frais impayés", e);
        }
    }

    private void computeAcademicStats(DashboardMetrics metrics) {
        try {
            List<Student> students = StudentService.getInstance().search(new StudentService.SearchCriteria());
            metrics.totalStudents = (int) students.stream()
                    .filter(s -> s.getStatut() == Student.StudentStatus.ACTIF)
                    .count();

            for (Student s : students) {
                if (s.getStatut() == Student.StudentStatus.ACTIF) {
                    String sectionName = s.getSectionName();
                    metrics.studentsBySection.merge(sectionName != null ? sectionName : "Non assigné", 1, Integer::sum);

                    String className = s.getClassName();
                    metrics.studentsByClass.merge(className != null ? className : "Non assigné", 1, Integer::sum);
                }
            }

            metrics.totalStaff = StaffService.getInstance().search(null, null, null, null).size();
            metrics.totalClasses = AcademicStructureService.getInstance().findAllClasses().size();
            metrics.totalOperations = HistoryService.getInstance().getAllOperations().size();
        } catch (Exception e) {
            logger.error("Erreur lors du calcul des effectifs scolaires", e);
        }
    }
}
