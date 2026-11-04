package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.Budget;

import java.util.ArrayList;
import java.util.List;

/**
 * BudgetService — Gestion et contrôle des lignes budgétaires.
 */
public class BudgetService {

    private static BudgetService instance;
    private final List<Budget> budgets = new ArrayList<>();

    private BudgetService() {
        initMockBudgets();
    }

    public static BudgetService getInstance() {
        if (instance == null) {
            instance = new BudgetService();
        }
        return instance;
    }

    private void initMockBudgets() {
        budgets.add(new Budget(1, "Fournitures & Papeterie", "BDG-01", "Pédagogique", 1500.0, 1350.0, "USD", "2025-2026", "ACTIF")); // 90% (alerte)
        budgets.add(new Budget(2, "Maintenance & Bâtiments", "BDG-02", "Infrastructure", 3000.0, 3100.0, "USD", "2025-2026", "DEPASSE")); // >100% (dépassé)
        budgets.add(new Budget(3, "Transport & Déplacements", "BDG-03", "Logistique", 800.0, 350.0, "USD", "2025-2026", "ACTIF"));
        budgets.add(new Budget(4, "Frais Événements & Cérémonies", "BDG-04", "Vie Scolaire", 5000000.0, 4100000.0, "CDF", "2025-2026", "ACTIF")); // 82% (alerte 80%)
        budgets.add(new Budget(5, "Pharmacie & Premiers Soins", "BDG-05", "Santé", 1200000.0, 400000.0, "CDF", "2025-2026", "ACTIF"));
    }

    public List<Budget> getAllBudgets() {
        return new ArrayList<>(budgets);
    }

    public List<Budget> getActiveBudgets() {
        List<Budget> list = new ArrayList<>();
        for (Budget b : budgets) {
            if ("ACTIF".equalsIgnoreCase(b.getStatus()) || "DEPASSE".equalsIgnoreCase(b.getStatus())) {
                list.add(b);
            }
        }
        return list;
    }

    public Budget findById(int id) {
        return budgets.stream().filter(b -> b.getId() == id).findFirst().orElse(null);
    }

    public Budget addBudget(Budget b) {
        if (b == null) throw new IllegalArgumentException("Budget requis.");
        b.setId(budgets.size() + 1);
        budgets.add(b);
        AuditService.getInstance().logAction("admin", "AJOUT_BUDGET", "Budget", null, b.getName() + " (" + b.getAllocatedAmount() + " " + b.getCurrency() + ")");
        return b;
    }

    public boolean hasSufficientBudget(Budget budget, double amount) {
        if (budget == null) return false;
        return budget.getAvailableAmount() >= amount;
    }

    public double calculateTotalAllocatedUSD() {
        return budgets.stream().filter(b -> "USD".equalsIgnoreCase(b.getCurrency())).mapToDouble(Budget::getAllocatedAmount).sum();
    }

    public double calculateTotalAllocatedCDF() {
        return budgets.stream().filter(b -> "CDF".equalsIgnoreCase(b.getCurrency())).mapToDouble(Budget::getAllocatedAmount).sum();
    }

    public double calculateTotalSpentUSD() {
        return budgets.stream().filter(b -> "USD".equalsIgnoreCase(b.getCurrency())).mapToDouble(Budget::getSpentAmount).sum();
    }

    public double calculateTotalSpentCDF() {
        return budgets.stream().filter(b -> "CDF".equalsIgnoreCase(b.getCurrency())).mapToDouble(Budget::getSpentAmount).sum();
    }

    public double getAvailableBudgetUSD() {
        return Math.max(0.0, calculateTotalAllocatedUSD() - calculateTotalSpentUSD());
    }

    public double getAvailableBudgetCDF() {
        return Math.max(0.0, calculateTotalAllocatedCDF() - calculateTotalSpentCDF());
    }
}
