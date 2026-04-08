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
public class PartnerResponse {
    private Long id;
    private String partnerId;
    private String webhookUrl;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
