package com.project.webhook_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventIngestionRequest {

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotBlank(message = "partnerId is required")
    private String partnerId;

    @NotNull(message = "eventType is required")
    private String eventType;

    /** Optional additional payload data. */
    private String payload;
}
