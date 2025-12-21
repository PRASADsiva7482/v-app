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
@Schema(description = "Function/action available for a menu")
public class RoleMenuFunctionBean {
    @Schema(description = "Function identifier")
    private Integer functionId;

    @Schema(description = "Associated menu ID")
    private Integer menuId;

    @Schema(description = "Function name")
    private String functionName;

    @Schema(description = "Scope/permission name")
    private String scopeName;

    @Schema(description = "Function description")
    private String functionDescription;
}
