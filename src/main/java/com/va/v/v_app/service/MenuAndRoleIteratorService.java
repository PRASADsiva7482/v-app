package com.va.v.v_app.service;

import com.va.v.v_app.model.RoleMenuBean;
import com.va.v.v_app.model.RoleMenuFunctionBean;
import com.va.v.v_app.persistence.primary.MenuDetailsMasterDomain;
import com.va.v.v_app.persistence.primary.MenuToFunctionMappingDomain;
import com.va.v.v_app.persistence.primary.MenuDetailsMasterRepo;
import com.va.v.v_app.persistence.primary.MenuToFunctionDetailsRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MenuAndRoleIteratorService {

    @Autowired
    private MenuDetailsMasterRepo menuDetailsRepo;

    @Autowired
    private MenuToFunctionDetailsRepo functionDetailsRepo;

    /**
     * Iterates through menus and functions to build role-based menu structure
     * 
     * @param resourceNames List of resource names from Keycloak permissions
     * @param menuLevel     Menu level to filter (0 for root menus)
     * @param scopes        List of scopes from Keycloak permissions
     * @return List of RoleMenuBean with functions mapped
     */
    public List<RoleMenuBean> iterateMenuAndRoleDetails(List<String> resourceNames, int menuLevel,
            List<String> scopes) {
        log.info("In Common Function to iterate menu and function repo");

        // Sort menus by parentId and menuOrder
        List<Order> orders = new ArrayList<>();
        orders.add(new Order(Direction.ASC, "parentId"));
        orders.add(new Order(Direction.ASC, "menuOrder"));

        // Fetch all functions and menus
        List<MenuToFunctionMappingDomain> functionRepoList = functionDetailsRepo.findAll();
        List<MenuDetailsMasterDomain> menuRepoList = menuDetailsRepo.findAll(Sort.by(orders));

        // Filter menus by resource names and menu level, then map functions
        List<RoleMenuBean> menuList = menuRepoList.stream()
                .filter(dao -> (resourceNames.contains(dao.getResourceName()) && dao.getMenuLevel() == menuLevel))
                .map(menu -> {
                    RoleMenuBean menuBean = mapMenuDomainToBean(menu);

                    // Map functions for this menu that match the scopes
                    List<RoleMenuFunctionBean> functionList = functionRepoList.stream()
                            .filter(function -> (function.getMenuId().equals(menu.getMenuId())
                                    && scopes.contains(function.getScopeName())))
                            .map(function -> {
                                RoleMenuFunctionBean roleFunctionBean = mapFunctionDomainToBean(function);
                                roleFunctionBean.setFunctionId(function.getSeqId());
                                return roleFunctionBean;
                            })
                            .collect(Collectors.toList());

                    menuBean.setFunctionIds(functionList);
                    return menuBean;
                })
                .collect(Collectors.toList());

        return menuList;
    }

    /**
     * Maps MenuDetailsMasterDomain to RoleMenuBean
     */
    private RoleMenuBean mapMenuDomainToBean(MenuDetailsMasterDomain domain) {
        RoleMenuBean bean = new RoleMenuBean();
        bean.setMenuId(domain.getMenuId());
        bean.setMenuName(domain.getMenuName());
        bean.setMenuPath(domain.getMenuPath());
        bean.setMenuIcon(domain.getMenuIcon());
        bean.setParentId(domain.getParentId());
        bean.setMenuOrder(domain.getMenuOrder());
        bean.setMenuLevel(domain.getMenuLevel());
        bean.setResourceName(domain.getResourceName());
        return bean;
    }

    /**
     * Maps MenuToFunctionMappingDomain to RoleMenuFunctionBean
     */
    private RoleMenuFunctionBean mapFunctionDomainToBean(MenuToFunctionMappingDomain domain) {
        RoleMenuFunctionBean bean = new RoleMenuFunctionBean();
        bean.setFunctionId(domain.getSeqId());
        bean.setMenuId(domain.getMenuId());
        bean.setFunctionName(domain.getFunctionName());
        bean.setScopeName(domain.getScopeName());
        bean.setFunctionDescription(domain.getFunctionDescription());
        return bean;
    }
}
