package com.va.v.v_app.persistence.primary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuToFunctionDetailsRepo extends JpaRepository<MenuToFunctionMappingDomain, Integer> {

    List<MenuToFunctionMappingDomain> findByIsActiveTrue();

    List<MenuToFunctionMappingDomain> findByMenuIdAndIsActiveTrue(Integer menuId);
}
