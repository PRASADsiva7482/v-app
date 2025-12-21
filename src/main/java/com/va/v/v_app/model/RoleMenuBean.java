package com.va.v.v_app.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleMenuBean {
    private Integer menuId;
    private String menuName;
    private String menuPath;
    private String menuIcon;
    private Integer parentId;
    private Integer menuOrder;
    private Integer menuLevel;
    private String resourceName;
    private List<RoleMenuFunctionBean> functionIds;
    private List<RoleMenuBean> childs;
}
