package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cursor pagination metadata
 * Used for efficient infinite scroll pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPagination {

    /**
     * Cursor for the next page (typically the ID of the last item)
     * Null if no more pages available
     */
    private String nextCursor;

    /**
     * Whether there are more items available
     */
    private boolean hasNext;

    /**
     * Requested page size
     */
    private int pageSize;

    /**
     * Actual number of items returned in this response
     */
    private int itemsInPage;

    /**
     * Total items loaded so far (optional, for progress indication)
     */
    private Integer totalLoaded;
}
