package com.va.v.v_app.v.service;

import com.va.v.v_app.v.exception.BusinessException;
import com.va.v.v_app.v.model.VerificationRequest;
import com.va.v.v_app.v.repository.VerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerificationRequestRepository verificationRequestRepository;

    @Transactional
    public Map<String, Object> submitRequest(String userId, String fullName, String category, String reason) {
        // Check if there's already a pending request
        var existing = verificationRequestRepository.findByUserIdAndStatus(userId, "PENDING");
        if (existing.isPresent()) {
            throw new BusinessException("ALREADY_PENDING", "You already have a pending verification request");
        }
        VerificationRequest request = VerificationRequest.builder()
                .userId(userId)
                .fullName(fullName)
                .category(category)
                .reason(reason)
                .build();
        VerificationRequest saved = verificationRequestRepository.save(request);
        return mapRequest(saved);
    }

    public List<Map<String, Object>> getMyRequests(String userId) {
        return verificationRequestRepository.findByUserId(userId)
                .stream().map(this::mapRequest).collect(Collectors.toList());
    }

    public Map<String, Object> getLatestRequest(String userId) {
        List<VerificationRequest> requests = verificationRequestRepository.findByUserId(userId);
        if (requests.isEmpty()) return Map.of("hasRequest", false);
        VerificationRequest latest = requests.get(requests.size() - 1);
        Map<String, Object> result = mapRequest(latest);
        result.put("hasRequest", true);
        return result;
    }

    private Map<String, Object> mapRequest(VerificationRequest r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("userId", r.getUserId());
        map.put("fullName", r.getFullName());
        map.put("category", r.getCategory());
        map.put("reason", r.getReason());
        map.put("status", r.getStatus());
        map.put("reviewerNotes", r.getReviewerNotes());
        map.put("createdAt", r.getCreatedAt());
        return map;
    }
}
