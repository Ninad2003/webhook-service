package com.project.webhook_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerRegistrationRequest {

    @NotBlank(message = "partnerId is required")
    private String partnerId;

    @NotBlank(message = "webhookUrl is required")
    @URL(message = "webhookUrl must be a valid URL")
    private String webhookUrl;
}
