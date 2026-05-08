package com.va.v.v_app.social.service;

import com.va.v.v_app.social.model.NotificationSubscription;
import com.va.v.v_app.social.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriptionService {

    private final NotificationSubscriptionRepository subscriptionRepository;

    @Transactional
    public Map<String, Object> subscribe(String subscriberId, String targetUserId) {
        if (subscriptionRepository.existsBySubscriberIdAndTargetUserId(subscriberId, targetUserId)) {
            return Map.of("subscribed", true, "message", "Already subscribed");
        }
        NotificationSubscription sub = NotificationSubscription.builder()
                .subscriberId(subscriberId).targetUserId(targetUserId).build();
        subscriptionRepository.save(sub);
        return Map.of("subscribed", true);
    }

    @Transactional
    public void unsubscribe(String subscriberId, String targetUserId) {
        subscriptionRepository.deleteBySubscriberIdAndTargetUserId(subscriberId, targetUserId);
    }

    public boolean isSubscribed(String subscriberId, String targetUserId) {
        return subscriptionRepository.existsBySubscriberIdAndTargetUserId(subscriberId, targetUserId);
    }

    public List<Map<String, Object>> getMySubscriptions(String subscriberId) {
        return subscriptionRepository.findBySubscriberId(subscriberId).stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("targetUserId", s.getTargetUserId());
            map.put("createdAt", s.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }
}
