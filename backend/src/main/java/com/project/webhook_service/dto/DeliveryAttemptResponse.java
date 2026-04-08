package com.project.webhook_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAttemptResponse {
    private Long id;
    private int attemptNumber;
    private Integer statusCode;
    private String responseBody;
    private Long responseTimeMs;
    private String error;
    private Instant createdAt;
}
