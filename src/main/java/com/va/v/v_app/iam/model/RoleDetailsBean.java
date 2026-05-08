package com.va.v.v_app.iam.model;

import java.util.List;

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
@Schema(description = "Role details containing menu structure")
public class RoleDetailsBean {

    @Schema(description = "List of menu items accessible to the user")
    private List<RoleMenuBean> menuIds;
}
