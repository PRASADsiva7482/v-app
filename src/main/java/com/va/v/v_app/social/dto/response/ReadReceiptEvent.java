package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket event for read receipt updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptEvent {

    private Long conversationId;
    private String readByUserId;
    private String newStatus; // DELIVERED or READ
    private int updatedCount;
}
