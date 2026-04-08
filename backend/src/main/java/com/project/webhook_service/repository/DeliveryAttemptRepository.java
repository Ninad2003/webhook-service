package com.project.webhook_service.repository;

import com.project.webhook_service.entity.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {

    List<DeliveryAttempt> findByWebhookEventIdOrderByAttemptNumberAsc(Long webhookEventId);
}
