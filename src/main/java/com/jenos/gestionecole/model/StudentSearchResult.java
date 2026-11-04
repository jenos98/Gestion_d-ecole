package com.jenos.gestionecole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * StudentSearchResult — Enveloppe paginée des résultats de recherche d'élèves.
 */
public class StudentSearchResult {

    private final List<Student> items;
    private final int totalCount;
    private final int page;
    private final int pageSize;
    private final int totalPages;

    public StudentSearchResult(List<Student> items, int totalCount, int page, int pageSize) {
        this.items = items != null ? items : new ArrayList<>();
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize > 0 ? pageSize : 25;
        this.totalPages = (totalCount > 0) ? (int) Math.ceil((double) totalCount / this.pageSize) : 1;
    }

    public List<Student> getItems() { return items; }
    public int getTotalCount() { return totalCount; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return totalPages; }

    public String getPageInfoText() {
        if (totalCount == 0) {
            return "0 élève trouvé";
        }
        int from = (page - 1) * pageSize + 1;
        int to = Math.min(totalCount, page * pageSize);
        return String.format("Élèves %d–%d sur %,d", from, to, totalCount);
    }
}
