package com.va.v.v_app.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.va.v.v_app.iam.model.MenuToFunctionMappingDomain;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuToFunctionDetailsRepo extends JpaRepository<MenuToFunctionMappingDomain, Integer> {

    List<MenuToFunctionMappingDomain> findByIsActiveTrue();

    List<MenuToFunctionMappingDomain> findByMenuIdAndIsActiveTrue(Integer menuId);
}
