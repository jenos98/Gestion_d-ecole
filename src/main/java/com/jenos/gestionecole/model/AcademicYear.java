package com.jenos.gestionecole.model;

import java.time.LocalDate;

/**
 * AcademicYear — Année scolaire et son cycle de vie.
 * États possibles : PLANIFIEE, ACTIVE, CLOTUREE, ARCHIVEE.
 * RÈGLE ABSOLUE : Une seule année active à la fois. Les anciennes années sont archivées (pas de suppression physique).
 */
public class AcademicYear {

    public enum YearStatus {
        PLANIFIEE,
        ACTIVE,
        CLOTUREE,
        ARCHIVEE
    }

    private int id;
    private String code;       // ex: "2025-2026"
    private String name;       // ex: "Année Scolaire 2025-2026"
    private LocalDate startDate;
    private LocalDate endDate;
    private YearStatus status;

    public AcademicYear() {
        this.status = YearStatus.PLANIFIEE;
    }

    public AcademicYear(int id, String code, String name, LocalDate startDate, LocalDate endDate, YearStatus status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public YearStatus getStatus() { return status; }
    public void setStatus(YearStatus status) { this.status = status; }

    public boolean isActive() {
        return status == YearStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return code + (status == YearStatus.ACTIVE ? " (En cours)" : " (" + status + ")");
    }
}
