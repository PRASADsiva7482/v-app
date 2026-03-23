package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.WatchParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchPartyRepository extends JpaRepository<WatchParty, Long> {
    List<WatchParty> findByStatusOrderByCreatedAtDesc(String status);
    List<WatchParty> findByHostIdOrderByCreatedAtDesc(String hostId);
    List<WatchParty> findTop20ByStatusOrderByParticipantCountDesc(String status);
}
