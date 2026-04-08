package com.project.webhook_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventIngestionResponse {
    private String eventId;
    private String status;
    private String message;
}
