package com.jenos.gestionecole.model;

/**
 * StudentFilter — Objet unifié et immuable représentant les critères de recherche,
 * filtrage et pagination des élèves dans tout le système (students.fxml, reports.fxml, exports).
 */
public class StudentFilter {

    public enum SortField {
        NOM("Nom complet"),
        MATRICULE("Matricule"),
        PROMOTION("Promotion"),
        OPTION("Option"),
        CLASSE("Classe"),
        ACADEMIC_YEAR("Année Scolaire"),
        STATUT("Statut");

        private final String label;

        SortField(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private String academicYear;
    private Section section;
    private Promotion promotion;
    private AcademicOption option;
    private Classe classe;
    private String searchTerm;
    private Student.StudentStatus status = Student.StudentStatus.ACTIF;
    private int page = 1;
    private int pageSize = 25; // 25, 50, 100
    private SortField sortField = SortField.NOM;
    private boolean sortAscending = true;

    public StudentFilter() {}

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public AcademicOption getOption() { return option; }
    public void setOption(AcademicOption option) { this.option = option; }

    public Classe getClasse() { return classe; }
    public void setClasse(Classe classe) { this.classe = classe; }

    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }

    public Student.StudentStatus getStatus() { return status; }
    public void setStatus(Student.StudentStatus status) { this.status = status; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize > 0 ? pageSize : 25; }

    public SortField getSortField() { return sortField; }
    public void setSortField(SortField sortField) { this.sortField = sortField; }

    public boolean isSortAscending() { return sortAscending; }
    public void setSortAscending(boolean sortAscending) { this.sortAscending = sortAscending; }

    public void setSort(SortField sortField, boolean sortAscending) {
        this.sortField = sortField;
        this.sortAscending = sortAscending;
    }

    public void reset() {
        this.academicYear = null;
        this.section = null;
        this.promotion = null;
        this.option = null;
        this.classe = null;
        this.searchTerm = null;
        this.status = null;
        this.page = 1;
        this.pageSize = 25;
        this.sortField = SortField.NOM;
        this.sortAscending = true;
    }
}
