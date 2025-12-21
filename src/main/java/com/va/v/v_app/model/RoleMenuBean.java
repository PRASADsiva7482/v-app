package com.va.v.v_app.model;

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
@Schema(description = "Menu item with permissions and child menus")
public class RoleMenuBean {
    @Schema(description = "Menu identifier")
    private Integer menuId;

    @Schema(description = "Display name of the menu")
    private String menuName;

    @Schema(description = "Navigation path/route")
    private String menuPath;

    @Schema(description = "Icon class or identifier")
    private String menuIcon;

    @Schema(description = "Parent menu ID (0 for root menus)")
    private Integer parentId;

    @Schema(description = "Display order")
    private Integer menuOrder;

    @Schema(description = "Menu hierarchy level")
    private Integer menuLevel;

    @Schema(description = "Resource name for authorization")
    private String resourceName;

    @Schema(description = "Available functions/actions for this menu")
    private List<RoleMenuFunctionBean> functionIds;

    @Schema(description = "Child/sub-menu items")
    private List<RoleMenuBean> childs;
}
