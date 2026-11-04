package cd.ecole.gestionscolaire.dto;

/**
 * PageRequest — Paramètres de pagination pour les requêtes de recherche.
 */
public class PageRequest {

    private int pageNumber; // 1-indexé
    private int pageSize;
    private String sortBy;
    private boolean ascending;

    public PageRequest() {
        this(1, 20);
    }

    public PageRequest(int pageNumber, int pageSize) {
        this.pageNumber = Math.max(1, pageNumber);
        this.pageSize = Math.max(1, pageSize);
        this.ascending = true;
    }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = Math.max(1, pageNumber); }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = Math.max(1, pageSize); }

    public int getOffset() {
        return (pageNumber - 1) * pageSize;
    }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public boolean isAscending() { return ascending; }
    public void setAscending(boolean ascending) { this.ascending = ascending; }
}
