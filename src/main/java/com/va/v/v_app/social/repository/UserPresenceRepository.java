package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, String> {
}
