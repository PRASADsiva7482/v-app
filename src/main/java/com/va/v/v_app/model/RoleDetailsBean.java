package com.va.v.v_app.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDetailsBean {
    private List<RoleMenuBean> menuIds;
}
