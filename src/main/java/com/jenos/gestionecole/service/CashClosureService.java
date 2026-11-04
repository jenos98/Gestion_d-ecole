package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.CashRegister;
import com.jenos.gestionecole.model.CashSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CashClosureService — Service de clôture et contrôle de caisse.
 *
 * Implémente la règle obligatoire :
 * Caisse ouverte → Rapport généré ? → Rapport imprimé ? → Comptage → Réconciliation → Confirmation → Clôturée
 */
public class CashClosureService {

    private static CashClosureService instance;

    private CashSession currentSession;

    private CashClosureService() {
        initDefaultSession();
    }

    public static CashClosureService getInstance() {
        if (instance == null) {
            instance = new CashClosureService();
        }
        return instance;
    }

    private void initDefaultSession() {
        CashRegister reg = new CashRegister(1, "Caisse Principale 01", "CS-01", "Jenos", CashRegister.Status.OPEN);
        currentSession = new CashSession();
        currentSession.setId(101);
        currentSession.setCashRegister(reg);
        currentSession.setCashierUsername("Jenos");
        currentSession.setInitialBalanceUSD(50.0);
        currentSession.setInitialBalanceCDF(100000.0);
        currentSession.setCurrentBalanceUSD(450.0);
        currentSession.setCurrentBalanceCDF(380000.0);
        currentSession.setClosureState(CashSession.ClosureState.OPEN);
    }

    public CashSession getCurrentSession() {
        return currentSession;
    }

    public List<CashRegister> getAllRegisters() {
        List<CashRegister> list = new ArrayList<>();
        list.add(new CashRegister(1, "Caisse Principale 01", "CS-01", "Jenos", CashRegister.Status.OPEN));
        list.add(new CashRegister(2, "Caisse Secondaire 02", "CS-02", "Marie", CashRegister.Status.OPEN));
        list.add(new CashRegister(3, "Caisse Frais Divers 03", "CS-03", null, CashRegister.Status.CLOSED));
        return list;
    }

    public boolean isDailyReportGenerated() {
        return currentSession != null && currentSession.isReportGenerated();
    }

    public boolean isDailyReportPrinted() {
        return currentSession != null && currentSession.isReportPrinted();
    }

    public boolean isClosureBlockedPendingDailyReport() {
        return currentSession != null && (!currentSession.isReportGenerated() || !currentSession.isReportPrinted());
    }

    /**
     * Étape 1 : Génération du rapport journalier
     */
    public void generateDailyReport() {
        if (currentSession == null) return;
        currentSession.setReportGenerated(true);
        currentSession.setClosureState(CashSession.ClosureState.REPORT_GENERATED);
    }

    /**
     * Étape 2 : Impression du rapport journalier
     */
    public void markDailyReportPrinted() {
        if (currentSession == null) return;
        if (!currentSession.isReportGenerated()) {
            throw new IllegalStateException("Le rapport journalier doit d'abord être généré avant d'être imprimé.");
        }
        currentSession.setReportPrinted(true);
        currentSession.setClosureState(CashSession.ClosureState.REPORT_PRINTED);
    }

    /**
     * Étape 3 & 4 : Comptage et réconciliation des montants physiques
     */
    public void recordCounting(double countedUSD, double countedCDF) {
        if (currentSession == null) return;
        if (!currentSession.isReportPrinted()) {
            throw new IllegalStateException("Le rapport journalier doit être imprimé avant de procéder au comptage.");
        }

        currentSession.setCountedBalanceUSD(countedUSD);
        currentSession.setCountedBalanceCDF(countedCDF);

        // Calcul des écarts sans conversion implicite
        currentSession.setDifferenceUSD(countedUSD - currentSession.getCurrentBalanceUSD());
        currentSession.setDifferenceCDF(countedCDF - currentSession.getCurrentBalanceCDF());

        currentSession.setClosureState(CashSession.ClosureState.RECONCILIATION);
    }

    /**
     * Étape 5 : Clôture définitive et verrouillage
     */
    public void finalizeClosure(String notes) {
        if (currentSession == null) return;
        if (!currentSession.isReportPrinted()) {
            throw new IllegalStateException("Le rapport doit impérativement être imprimé pour clôturer la caisse.");
        }

        currentSession.setClosureNotes(notes);
        currentSession.setClosedAt(LocalDateTime.now());
        currentSession.setClosureState(CashSession.ClosureState.CLOSED);
        currentSession.getCashRegister().setStatus(CashRegister.Status.CLOSED);
    }

    /**
     * Clôture exceptionnelle avec motif et autorisation superviseur
     */
    public void forceClosure(String reason, String authorizedBy) {
        if (currentSession == null) return;
        currentSession.setExceptionalClosure(true);
        currentSession.setClosureNotes("CLÔTURE EXCEPTIONNELLE autorisée par: " + authorizedBy + ". Motif: " + reason);
        currentSession.setClosedAt(LocalDateTime.now());
        currentSession.setClosureState(CashSession.ClosureState.CLOSED);
        currentSession.getCashRegister().setStatus(CashRegister.Status.CLOSED);
    }
}
