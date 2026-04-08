package com.project.webhook_service.service;

import com.project.webhook_service.dto.DeliveryAttemptResponse;
import com.project.webhook_service.dto.EventDetailResponse;
import com.project.webhook_service.dto.EventResponse;
import com.project.webhook_service.entity.DeliveryStatus;
import com.project.webhook_service.entity.EventType;
import com.project.webhook_service.entity.WebhookEvent;
import com.project.webhook_service.exception.EventNotFoundException;
import com.project.webhook_service.repository.WebhookEventRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final WebhookEventRepository webhookEventRepository;

    @Transactional(readOnly = true)
    public Page<EventResponse> listEvents(String partnerId, String status, String eventType, Pageable pageable) {
        Specification<WebhookEvent> spec = buildSpec(partnerId, status, eventType);
        return webhookEventRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEventDetail(Long id) {
        WebhookEvent event = webhookEventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        return toDetailResponse(event);
    }

    private Specification<WebhookEvent> buildSpec(String partnerId, String status, String eventType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (partnerId != null && !partnerId.isBlank()) {
                predicates.add(cb.equal(root.get("partnerId"), partnerId));
            }
            if (status != null && !status.isBlank()) {
                try {
                    DeliveryStatus deliveryStatus = DeliveryStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), deliveryStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status filter: {}", status);
                }
            }
            if (eventType != null && !eventType.isBlank()) {
                try {
                    EventType type = EventType.valueOf(eventType.toUpperCase());
                    predicates.add(cb.equal(root.get("eventType"), type));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid eventType filter: {}", eventType);
                }
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private EventResponse toResponse(WebhookEvent event) {
        return EventResponse.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .transactionId(event.getTransactionId())
                .partnerId(event.getPartnerId())
                .eventType(event.getEventType().name())
                .status(event.getStatus().name())
                .attemptCount(event.getAttemptCount())
                .maxAttempts(event.getMaxAttempts())
                .sequenceNumber(event.getSequenceNumber())
                .nextRetryAt(event.getNextRetryAt())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private EventDetailResponse toDetailResponse(WebhookEvent event) {
        List<DeliveryAttemptResponse> attempts = event.getDeliveryAttempts().stream()
                .map(a -> DeliveryAttemptResponse.builder()
                        .id(a.getId())
                        .attemptNumber(a.getAttemptNumber())
                        .statusCode(a.getStatusCode())
                        .responseBody(a.getResponseBody())
                        .responseTimeMs(a.getResponseTimeMs())
                        .error(a.getError())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return EventDetailResponse.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .transactionId(event.getTransactionId())
                .partnerId(event.getPartnerId())
                .eventType(event.getEventType().name())
                .payload(event.getPayload())
                .status(event.getStatus().name())
                .attemptCount(event.getAttemptCount())
                .maxAttempts(event.getMaxAttempts())
                .sequenceNumber(event.getSequenceNumber())
                .nextRetryAt(event.getNextRetryAt())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .deliveryAttempts(attempts)
                .build();
    }
}
