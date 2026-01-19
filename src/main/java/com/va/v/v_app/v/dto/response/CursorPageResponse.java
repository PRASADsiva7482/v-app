package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic cursor-based page response
 * Used for infinite scroll pagination
 * 
 * @param <T> Type of data items in the page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponse<T> {

    /**
     * List of items in this page
     */
    private List<T> data;

    /**
     * Pagination metadata
     */
    private CursorPagination pagination;

    /**
     * Optional metadata about the response
     */
    private Object metadata;

    /**
     * Create a cursor page response
     */
    public static <T> CursorPageResponse<T> of(List<T> data, String nextCursor, int pageSize) {
        CursorPagination pagination = CursorPagination.builder()
                .nextCursor(nextCursor)
                .hasNext(nextCursor != null)
                .pageSize(pageSize)
                .itemsInPage(data.size())
                .build();

        return CursorPageResponse.<T>builder()
                .data(data)
                .pagination(pagination)
                .build();
    }

    /**
     * Create a cursor page response with metadata
     */
    public static <T> CursorPageResponse<T> of(List<T> data, String nextCursor, int pageSize, Object metadata) {
        CursorPageResponse<T> response = of(data, nextCursor, pageSize);
        response.setMetadata(metadata);
        return response;
    }
}
