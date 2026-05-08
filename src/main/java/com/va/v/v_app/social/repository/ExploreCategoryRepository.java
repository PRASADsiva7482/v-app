package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.ExploreCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ExploreCategory entity
 */
@Repository
public interface ExploreCategoryRepository extends JpaRepository<ExploreCategory, Long> {

    Optional<ExploreCategory> findByName(String name);

    List<ExploreCategory> findByIsActiveTrueOrderBySortOrderAsc();
}
