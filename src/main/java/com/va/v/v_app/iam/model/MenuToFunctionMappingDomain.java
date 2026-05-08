package com.va.v.v_app.iam.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "menu_to_function_mapping")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuToFunctionMappingDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq_id")
    private Integer seqId;

    @Column(name = "menu_id")
    private Integer menuId;

    @Column(name = "function_name", length = 100)
    private String functionName;

    @Column(name = "scope_name", length = 100)
    private String scopeName;

    @Column(name = "function_description", length = 255)
    private String functionDescription;

    @Column(name = "is_active")
    private Boolean isActive;
}
