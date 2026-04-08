package com.project.webhook_service.service;

import com.project.webhook_service.dto.EventIngestionRequest;
import com.project.webhook_service.dto.EventIngestionResponse;
import com.project.webhook_service.entity.DeliveryStatus;
import com.project.webhook_service.entity.EventType;
import com.project.webhook_service.entity.WebhookEvent;
import com.project.webhook_service.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestionService {

    private final WebhookEventRepository webhookEventRepository;
    private final PartnerService partnerService;

    /**
     * Ingest an event from the upstream screening engine.
     * - Validates the event type and partner existence.
     * - Computes a deterministic idempotency key from (transactionId, partnerId, eventType).
     * - Deduplicates: if an event with this key already exists, returns idempotent success.
     * - Assigns a per-partner sequence number for ordering.
     * - Persists the event as PENDING for async delivery.
     */
    @Transactional
    public EventIngestionResponse ingestEvent(EventIngestionRequest request) {
        // Validate event type
        EventType eventType;
        try {
            eventType = EventType.valueOf(request.getEventType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid event type: " + request.getEventType() +
                    ". Valid types: KYC_REGISTERED, TXN_SCREENED, TXN_BLOCKED, TXN_RELEASED, INVALID_TXN");
        }

        // Validate partner exists
        partnerService.getPartnerEntity(request.getPartnerId());

        // Strict Business Logic Validation: Cannot RELEASE a transaction that was never BLOCKED
        if (eventType == EventType.TXN_RELEASED) {
            boolean wasBlocked = webhookEventRepository.existsByTransactionIdAndEventType(
                    request.getTransactionId(), EventType.TXN_BLOCKED);
            if (!wasBlocked) {
                throw new IllegalArgumentException("Strict Validation Error: Cannot ingest a TXN_RELEASED event for a transaction that was never BLOCKED.");
            }
        }

        // Compute idempotency key
        String eventId = computeEventId(request.getTransactionId(), request.getPartnerId(), request.getEventType());

        // Deduplication check
        if (webhookEventRepository.existsByEventId(eventId)) {
            log.info("Duplicate event detected, returning idempotent success: {}", eventId);
            return EventIngestionResponse.builder()
                    .eventId(eventId)
                    .status("ACCEPTED")
                    .message("Event already ingested (deduplicated)")
                    .build();
        }

        // Assign per-partner sequence number
        Long maxSeq = webhookEventRepository.findMaxSequenceNumberByPartnerId(request.getPartnerId());
        long nextSeq = (maxSeq == null) ? 1 : maxSeq + 1;

        // Build payload JSON
        String payload = buildPayload(request, eventType);

        // Persist event
        WebhookEvent event = WebhookEvent.builder()
                .eventId(eventId)
                .transactionId(request.getTransactionId())
                .partnerId(request.getPartnerId())
                .eventType(eventType)
                .payload(payload)
                .status(DeliveryStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(5)
                .nextRetryAt(Instant.now())
                .sequenceNumber(nextSeq)
                .build();

        webhookEventRepository.save(event);
        log.info("Event ingested: eventId={}, partnerId={}, seq={}, type={}",
                eventId, request.getPartnerId(), nextSeq, eventType);

        return EventIngestionResponse.builder()
                .eventId(eventId)
                .status("ACCEPTED")
                .message("Event accepted for delivery")
                .build();
    }

    /**
     * Deterministic hash of (transactionId, partnerId, eventType) to produce an idempotency key.
     */
    private String computeEventId(String transactionId, String partnerId, String eventType) {
        try {
            String raw = transactionId + "|" + partnerId + "|" + eventType;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String buildPayload(EventIngestionRequest request, EventType eventType) {
        return String.format(
                "{\"transaction_id\":\"%s\",\"partner_id\":\"%s\",\"event_type\":\"%s\",\"timestamp\":\"%s\"%s}",
                request.getTransactionId(),
                request.getPartnerId(),
                eventType.name(),
                Instant.now().toString(),
                request.getPayload() != null ? ",\"data\":" + request.getPayload() : ""
        );
    }
}
