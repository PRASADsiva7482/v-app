package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single poll option
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollOptionResponse {

    private Long id;
    private String optionText;
    private Integer position;
    private Integer voteCount;
    private Double percentage; // Calculated percentage of total votes
}
