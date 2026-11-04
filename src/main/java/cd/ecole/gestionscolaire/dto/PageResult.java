package cd.ecole.gestionscolaire.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PageResult — Conteneur générique de résultats paginés.
 */
public class PageResult<T> {

    private final List<T> items;
    private final int totalCount;
    private final int pageNumber;
    private final int pageSize;
    private final int totalPages;

    public PageResult(List<T> items, int totalCount, int pageNumber, int pageSize) {
        this.items = items != null ? items : Collections.emptyList();
        this.totalCount = totalCount;
        this.pageNumber = pageNumber;
        this.pageSize = Math.max(1, pageSize);
        this.totalPages = (int) Math.ceil((double) totalCount / this.pageSize);
    }

    public static <T> PageResult<T> empty(int pageNumber, int pageSize) {
        return new PageResult<>(new ArrayList<>(), 0, pageNumber, pageSize);
    }

    public List<T> getItems() { return items; }
    public int getTotalCount() { return totalCount; }
    public int getPageNumber() { return pageNumber; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return totalPages; }
    public boolean hasNext() { return pageNumber < totalPages; }
    public boolean hasPrevious() { return pageNumber > 1; }
}
