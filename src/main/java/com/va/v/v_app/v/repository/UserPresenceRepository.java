package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, String> {
}
