package com.va.v.v_app.social.service;

import com.va.v.v_app.social.model.WatchParty;
import com.va.v.v_app.social.repository.WatchPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchPartyService {

    private final WatchPartyRepository watchPartyRepository;

    public WatchParty createParty(WatchParty party) {
        return watchPartyRepository.save(party);
    }

    public List<WatchParty> getActiveParties() {
        return watchPartyRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
    }

    public List<WatchParty> getPopularParties() {
        return watchPartyRepository.findTop20ByStatusOrderByParticipantCountDesc("ACTIVE");
    }

    public List<WatchParty> getUserParties(String hostId) {
        return watchPartyRepository.findByHostIdOrderByCreatedAtDesc(hostId);
    }

    public WatchParty joinParty(Long partyId) {
        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));
        party.setParticipantCount(party.getParticipantCount() + 1);
        return watchPartyRepository.save(party);
    }

    public WatchParty leaveParty(Long partyId) {
        WatchParty party = watchPartyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("Watch party not found"));
        party.setParticipantCount(Math.max(0, party.getParticipantCount() - 1));
        return watchPartyRepository.save(party);
    }

    public void endParty(Long partyId, String userId) {
        watchPartyRepository.findById(partyId).ifPresent(party -> {
            if (party.getHostId().equals(userId)) {
                party.setStatus("ENDED");
                party.setEndedAt(LocalDateTime.now());
                watchPartyRepository.save(party);
            }
        });
    }
}
