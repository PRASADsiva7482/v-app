package com.va.v.v_app.persistence.primary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuDetailsMasterRepo extends JpaRepository<MenuDetailsMasterDomain, Integer> {

    List<MenuDetailsMasterDomain> findByIsActiveTrue();

    List<MenuDetailsMasterDomain> findByResourceNameIn(List<String> resourceNames);
}
