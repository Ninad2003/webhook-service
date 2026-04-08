package com.project.webhook_service.service;

import com.project.webhook_service.dto.EventIngestionRequest;
import com.project.webhook_service.dto.EventIngestionResponse;
import com.project.webhook_service.entity.DeliveryStatus;
import com.project.webhook_service.entity.EventType;
import com.project.webhook_service.entity.WebhookEvent;
import com.project.webhook_service.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EventIngestionService {

    private final WebhookEventRepository webhookEventRepository;
    private final PartnerService partnerService;

    @Transactional
    public EventIngestionResponse ingestEvent(EventIngestionRequest request) {
        EventType eventType;
        try {
            eventType = EventType.valueOf(request.getEventType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid event type: " + request.getEventType() +
                    ". Valid types: KYC_REGISTERED, TXN_SCREENED, TXN_BLOCKED, TXN_RELEASED, INVALID_TXN");
        }

        partnerService.getPartnerEntity(request.getPartnerId());

        if (eventType == EventType.TXN_RELEASED) {
            boolean wasBlocked = webhookEventRepository.existsByTransactionIdAndEventType(
                    request.getTransactionId(), EventType.TXN_BLOCKED);
            if (!wasBlocked) {
                throw new IllegalArgumentException("Strict Validation Error: Cannot ingest a TXN_RELEASED event for a transaction that was never BLOCKED.");
            }
        }

        String eventId = computeEventId(request.getTransactionId(), request.getPartnerId(), request.getEventType());

        if (webhookEventRepository.existsByEventId(eventId)) {
            return EventIngestionResponse.builder()
                    .eventId(eventId)
                    .status("ACCEPTED")
                    .message("Event already ingested (deduplicated)")
                    .build();
        }

        Long maxSeq = webhookEventRepository.findMaxSequenceNumberByPartnerId(request.getPartnerId());
        long nextSeq = (maxSeq == null) ? 1 : maxSeq + 1;

        String payload = buildPayload(request, eventType);

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

        return EventIngestionResponse.builder()
                .eventId(eventId)
                .status("ACCEPTED")
                .message("Event accepted for delivery")
                .build();
    }

    private String computeEventId(String transactionId, String partnerId, String eventType) {
        return transactionId + "-" + partnerId + "-" + eventType;
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
