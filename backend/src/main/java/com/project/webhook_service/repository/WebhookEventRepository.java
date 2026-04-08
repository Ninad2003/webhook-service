package com.project.webhook_service.repository;

import com.project.webhook_service.entity.DeliveryStatus;
import com.project.webhook_service.entity.EventType;
import com.project.webhook_service.entity.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByEventId(String eventId);

    boolean existsByTransactionIdAndEventType(String transactionId, EventType eventType);

    Optional<WebhookEvent> findByEventId(String eventId);

    @Query("SELECT MAX(e.sequenceNumber) FROM WebhookEvent e WHERE e.partnerId = :partnerId")
    Long findMaxSequenceNumberByPartnerId(@Param("partnerId") String partnerId);

    @Query("SELECT DISTINCT e.partnerId FROM WebhookEvent e WHERE e.status = 'PENDING' AND e.nextRetryAt <= :now")
    List<String> findPartnersWithDeliverableEvents(@Param("now") Instant now);

    Optional<WebhookEvent> findFirstByPartnerIdAndStatusAndNextRetryAtLessThanEqualOrderBySequenceNumberAsc(String partnerId, DeliveryStatus status, Instant now);

    default Optional<WebhookEvent> findNextDeliverableEvent(String partnerId, Instant now) {
        return findFirstByPartnerIdAndStatusAndNextRetryAtLessThanEqualOrderBySequenceNumberAsc(partnerId, DeliveryStatus.PENDING, now);
    }

    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query("UPDATE WebhookEvent e SET e.status = 'PENDING', e.nextRetryAt = :now " +
            "WHERE e.status = 'PROCESSING'")
    int resetStaleProcessingEvents(@Param("now") Instant now);

    Page<WebhookEvent> findByPartnerId(String partnerId, Pageable pageable);

    Page<WebhookEvent> findByStatus(DeliveryStatus status, Pageable pageable);

    Page<WebhookEvent> findByEventType(EventType eventType, Pageable pageable);

    Page<WebhookEvent> findByPartnerIdAndStatus(String partnerId, DeliveryStatus status, Pageable pageable);

    @Query("SELECT e FROM WebhookEvent e WHERE " +
           "(:partnerId IS NULL OR e.partnerId = :partnerId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:eventType IS NULL OR e.eventType = :eventType)")
    Page<WebhookEvent> findEventsWithFilters(
            @Param("partnerId") String partnerId,
            @Param("status") DeliveryStatus status,
            @Param("eventType") EventType eventType,
            Pageable pageable);
}
