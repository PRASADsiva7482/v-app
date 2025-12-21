package com.va.v.v_app.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@Schema(description = "User details information from Keycloak")
public class UserDetailsBean {

    @Schema(description = "Unique user identifier", example = "abc-123-def-456")
    private String userId;

    @Schema(description = "Username", example = "john.doe")
    private String userName;

    @Schema(description = "Full name of the user", example = "John Doe")
    private String fullName;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String emailId;

    @Schema(description = "MVNO identifier")
    private String mvnoId;

    @Schema(description = "Login type")
    private String loginType;

    @Schema(description = "Color code for UI customization")
    private String colorCode;

    @Schema(description = "File data")
    private String fileData;

    @Schema(description = "Group names the user belongs to", example = "admin,users")
    private String groupNames;

    @Schema(description = "UMS status code")
    private String umsStatusCode;

    @Schema(description = "MVNO status code")
    private String mvnoStatusCode;

    @Schema(description = "MVNO attach status code")
    private String mvnoAttachStatusCode;

    @Schema(description = "Role details and menu structure")
    private RoleDetailsBean roleDetails;
}
