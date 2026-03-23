package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {
    Optional<NotificationSubscription> findBySubscriberIdAndTargetUserId(String subscriberId, String targetUserId);
    List<NotificationSubscription> findBySubscriberId(String subscriberId);
    List<NotificationSubscription> findByTargetUserId(String targetUserId);
    boolean existsBySubscriberIdAndTargetUserId(String subscriberId, String targetUserId);
    void deleteBySubscriberIdAndTargetUserId(String subscriberId, String targetUserId);
}
