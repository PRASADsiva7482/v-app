package com.va.v.v_app.iam.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menu_details_master")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuDetailsMasterDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Integer menuId;

    @Column(name = "menu_name", length = 100)
    private String menuName;

    @Column(name = "menu_path", length = 255)
    private String menuPath;

    @Column(name = "menu_icon", length = 100)
    private String menuIcon;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "menu_order")
    private Integer menuOrder;

    @Column(name = "menu_level")
    private Integer menuLevel;

    @Column(name = "resource_name", length = 100)
    private String resourceName;

    @Column(name = "is_active")
    private Boolean isActive;
}
