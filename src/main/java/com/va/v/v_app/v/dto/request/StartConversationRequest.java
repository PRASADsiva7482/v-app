package com.va.v.v_app.v.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to start a new conversation (1:1 or group)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartConversationRequest {

    /** Keycloak user_id of the other participant (for DIRECT) */
    private String recipientUserId;

    /** For GROUP conversations — list of user IDs to add */
    private List<String> participantIds;

    /** Group name (required for GROUP type) */
    private String groupName;

    /** Group avatar URL */
    private String groupAvatarUrl;

    /** Conversation type: DIRECT or GROUP */
    private String type;
}
