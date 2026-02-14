package com.va.v.v_app.v.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to start a new 1:1 conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartConversationRequest {

    /** Keycloak user_id of the other participant */
    private String recipientUserId;
}
