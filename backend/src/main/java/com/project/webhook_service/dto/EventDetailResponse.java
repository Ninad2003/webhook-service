package com.project.webhook_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetailResponse {
    private Long id;
    private String eventId;
    private String transactionId;
    private String partnerId;
    private String eventType;
    private String payload;
    private String status;
    private int attemptCount;
    private int maxAttempts;
    private Long sequenceNumber;
    private Instant nextRetryAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<DeliveryAttemptResponse> deliveryAttempts;
}
