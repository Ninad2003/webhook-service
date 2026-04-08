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
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long>, JpaSpecificationExecutor<WebhookEvent> {

    /** Check if an event with this idempotency key already exists. */
    boolean existsByEventId(String eventId);

    /** Used for strict state validation (e.g. checking if a TXN was BLOCKED before RELEASED) */
    boolean existsByTransactionIdAndEventType(String transactionId, EventType eventType);

    Optional<WebhookEvent> findByEventId(String eventId);

    /**
     * Get the max sequence number for a given partner so we can assign the next one.
     * Returns null if no events exist for this partner yet.
     */
    @Query("SELECT MAX(e.sequenceNumber) FROM WebhookEvent e WHERE e.partnerId = :partnerId")
    Long findMaxSequenceNumberByPartnerId(@Param("partnerId") String partnerId);

    /**
     * Find distinct partner IDs that have pending deliverable events.
     * An event is deliverable if it is PENDING and nextRetryAt <= now.
     */
    @Query("SELECT DISTINCT e.partnerId FROM WebhookEvent e WHERE e.status = 'PENDING' AND e.nextRetryAt <= :now")
    List<String> findPartnersWithDeliverableEvents(@Param("now") Instant now);

    /**
     * For a given partner, find the next deliverable event (lowest sequence number that is PENDING
     * and ready for retry). This ensures per-partner ordering.
     */
    Optional<WebhookEvent> findFirstByPartnerIdAndStatusAndNextRetryAtLessThanEqualOrderBySequenceNumberAsc(String partnerId, DeliveryStatus status, Instant now);

    default Optional<WebhookEvent> findNextDeliverableEvent(String partnerId, Instant now) {
        return findFirstByPartnerIdAndStatusAndNextRetryAtLessThanEqualOrderBySequenceNumberAsc(partnerId, DeliveryStatus.PENDING, now);
    }

    /**
     * Reset stale PROCESSING events back to PENDING after a crash.
     * Called on startup to handle events that were being processed when the service died.
     */
    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query("UPDATE WebhookEvent e SET e.status = 'PENDING', e.nextRetryAt = :now " +
            "WHERE e.status = 'PROCESSING'")
    int resetStaleProcessingEvents(@Param("now") Instant now);

    /** Dashboard filtering queries. */
    Page<WebhookEvent> findByPartnerId(String partnerId, Pageable pageable);

    Page<WebhookEvent> findByStatus(DeliveryStatus status, Pageable pageable);

    Page<WebhookEvent> findByEventType(EventType eventType, Pageable pageable);

    Page<WebhookEvent> findByPartnerIdAndStatus(String partnerId, DeliveryStatus status, Pageable pageable);
}
