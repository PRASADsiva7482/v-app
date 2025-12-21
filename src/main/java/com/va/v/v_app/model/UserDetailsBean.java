package com.va.v.v_app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsBean {
    private String userId;
    private String userName;
    private String fullName;
    private String emailId;
    private String mvnoId;
    private String loginType;
    private String colorCode;
    private String fileData;
    private String groupNames;
    private String umsStatusCode;
    private String mvnoStatusCode;
    private String mvnoAttachStatusCode;
    private RoleDetailsBean roleDetails;
}
