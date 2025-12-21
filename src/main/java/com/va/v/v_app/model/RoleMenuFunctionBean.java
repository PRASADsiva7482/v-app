package com.va.v.v_app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleMenuFunctionBean {
    private Integer functionId;
    private Integer menuId;
    private String functionName;
    private String scopeName;
    private String functionDescription;
}
