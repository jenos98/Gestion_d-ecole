package com.jenos.gestionecole.model;

import java.time.LocalDateTime;

/**
 * ReportLog — Traçabilité et historique d'émission d'un rapport.
 */
public class ReportLog {

    public enum ReportStatus {
        CREE,
        GENERE,
        PREVISUALISE,
        IMPRIME,
        EXPORTE,
        ARCHIVE
    }

    private String reportId;
    private String typeLabel;
    private String generatedBy;
    private LocalDateTime generatedAt;
    private String period;
    private String cashRegisterName;
    private ReportStatus status;
    private LocalDateTime printedAt;
    private LocalDateTime exportedAt;

    public ReportLog(String reportId, String typeLabel, String generatedBy, LocalDateTime generatedAt, String period, String cashRegisterName, ReportStatus status) {
        this.reportId = reportId;
        this.typeLabel = typeLabel;
        this.generatedBy = generatedBy;
        this.generatedAt = generatedAt;
        this.period = period;
        this.cashRegisterName = cashRegisterName;
        this.status = status;
    }

    public String getReportId() { return reportId; }
    public String getTypeLabel() { return typeLabel; }
    public String getGeneratedBy() { return generatedBy; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getPeriod() { return period; }
    public String getCashRegisterName() { return cashRegisterName; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public LocalDateTime getPrintedAt() { return printedAt; }
    public void setPrintedAt(LocalDateTime printedAt) { this.printedAt = printedAt; }
    public LocalDateTime getExportedAt() { return exportedAt; }
    public void setExportedAt(LocalDateTime exportedAt) { this.exportedAt = exportedAt; }
}
