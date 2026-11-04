package com.jenos.gestionecole.service;

import com.jenos.gestionecole.model.AcademicYear;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AcademicYearService — Gestion et cycle de vie des années scolaires.
 * RÈGLE ABSOLUE : Une seule année scolaire active à la fois.
 * L'ancienne année devient ARCHIVÉE (aucune suppression physique).
 */
public class AcademicYearService {

    private static AcademicYearService instance;
    private final List<AcademicYear> years = new ArrayList<>();

    private AcademicYearService() {
        initMockYears();
    }

    public static AcademicYearService getInstance() {
        if (instance == null) {
            instance = new AcademicYearService();
        }
        return instance;
    }

    private void initMockYears() {
        years.add(new AcademicYear(1, "2024-2025", "Année Scolaire 2024-2025",
                LocalDate.of(2024, 9, 2), LocalDate.of(2025, 7, 2), AcademicYear.YearStatus.ARCHIVEE));

        years.add(new AcademicYear(2, "2025-2026", "Année Scolaire 2025-2026",
                LocalDate.of(2025, 9, 1), LocalDate.of(2026, 7, 2), AcademicYear.YearStatus.ACTIVE));

        years.add(new AcademicYear(3, "2026-2027", "Année Scolaire 2026-2027",
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 7, 2), AcademicYear.YearStatus.PLANIFIEE));
    }

    public List<AcademicYear> getAllYears() {
        return new ArrayList<>(years);
    }

    public AcademicYear getActiveYear() {
        return years.stream()
                .filter(AcademicYear::isActive)
                .findFirst()
                .orElse(null);
    }

    public AcademicYear findById(int id) {
        return years.stream().filter(y -> y.getId() == id).findFirst().orElse(null);
    }

    public AcademicYear createYear(String code, String name, LocalDate start, LocalDate end) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Le code est obligatoire (ex: 2027-2028).");
        for (AcademicYear y : years) {
            if (code.equalsIgnoreCase(y.getCode())) {
                throw new IllegalArgumentException("L'année scolaire " + code + " existe déjà.");
            }
        }

        AcademicYear year = new AcademicYear(years.size() + 1, code, name, start, end, AcademicYear.YearStatus.PLANIFIEE);
        years.add(year);
        AuditService.getInstance().logAction("admin", "CRÉATION_ANNÉE_SCOLAIRE", "AcademicYear", null, code);
        return year;
    }

    /**
     * Active une année scolaire. L'année active actuelle passe automatiquement au statut ARCHIVEE.
     */
    public void activateYear(int id) {
        AcademicYear target = findById(id);
        if (target == null) throw new IllegalArgumentException("Année introuvable.");

        AcademicYear currentActive = getActiveYear();
        if (currentActive != null && currentActive.getId() != target.getId()) {
            currentActive.setStatus(AcademicYear.YearStatus.ARCHIVEE);
        }

        target.setStatus(AcademicYear.YearStatus.ACTIVE);
        AuditService.getInstance().logAction("admin", "ACTIVATION_ANNÉE_SCOLAIRE", "AcademicYear",
                currentActive != null ? currentActive.getCode() : "Aucune", target.getCode());
    }

    public void closeYear(int id) {
        AcademicYear target = findById(id);
        if (target == null) throw new IllegalArgumentException("Année introuvable.");
        target.setStatus(AcademicYear.YearStatus.CLOTUREE);
        AuditService.getInstance().logAction("admin", "CLÔTURE_ANNÉE_SCOLAIRE", "AcademicYear", target.getCode(), "CLOTUREE");
    }

    public void archiveYear(int id) {
        AcademicYear target = findById(id);
        if (target == null) throw new IllegalArgumentException("Année introuvable.");
        target.setStatus(AcademicYear.YearStatus.ARCHIVEE);
        AuditService.getInstance().logAction("admin", "ARCHIVAGE_ANNÉE_SCOLAIRE", "AcademicYear", target.getCode(), "ARCHIVEE");
    }
}
