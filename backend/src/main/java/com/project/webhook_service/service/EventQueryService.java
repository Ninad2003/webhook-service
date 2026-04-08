package com.project.webhook_service.service;

import com.project.webhook_service.dto.DeliveryAttemptResponse;
import com.project.webhook_service.dto.EventDetailResponse;
import com.project.webhook_service.dto.EventResponse;
import com.project.webhook_service.entity.DeliveryStatus;
import com.project.webhook_service.entity.EventType;
import com.project.webhook_service.entity.WebhookEvent;
import com.project.webhook_service.exception.EventNotFoundException;
import com.project.webhook_service.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final WebhookEventRepository webhookEventRepository;

    @Transactional(readOnly = true)
    public Page<EventResponse> listEvents(String partnerId, String status, String eventType, Pageable pageable) {
        
        String filterPartnerId = (partnerId != null && !partnerId.isBlank()) ? partnerId : null;
        
        DeliveryStatus filterStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                filterStatus = DeliveryStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        
        EventType filterEventType = null;
        if (eventType != null && !eventType.isBlank()) {
            try {
                filterEventType = EventType.valueOf(eventType.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }

        return webhookEventRepository.findEventsWithFilters(filterPartnerId, filterStatus, filterEventType, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEventDetail(Long id) {
        WebhookEvent event = webhookEventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        return toDetailResponse(event);
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
