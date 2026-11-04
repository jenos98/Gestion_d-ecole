package com.jenos.gestionecole.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportDocument — Structure de données pour la prévisualisation et l'impression d'un rapport.
 */
public class ReportDocument {

    private String title;
    private String schoolName = "Établissement Scolaire";
    private LocalDateTime generatedAt = LocalDateTime.now();
    private String generatedBy = "Jenos";
    private String filterSummary;
    private List<String> headers = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
    private String totalUSD;
    private String totalCDF;
    private boolean landscape;

    public ReportDocument() {}

    public ReportDocument(String title, String filterSummary) {
        this.title = title;
        this.filterSummary = filterSummary;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    public String getFilterSummary() { return filterSummary; }
    public void setFilterSummary(String filterSummary) { this.filterSummary = filterSummary; }

    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }

    public List<List<String>> getRows() { return rows; }
    public void setRows(List<List<String>> rows) { this.rows = rows; }

    public String getTotalUSD() { return totalUSD; }
    public void setTotalUSD(String totalUSD) { this.totalUSD = totalUSD; }

    public String getTotalCDF() { return totalCDF; }
    public void setTotalCDF(String totalCDF) { this.totalCDF = totalCDF; }

    public boolean isLandscape() { return landscape; }
    public void setLandscape(boolean landscape) { this.landscape = landscape; }
}
