package com.project.webhook_service.service;

import com.project.webhook_service.dto.PartnerRegistrationRequest;
import com.project.webhook_service.dto.PartnerResponse;
import com.project.webhook_service.entity.Partner;
import com.project.webhook_service.exception.PartnerNotFoundException;
import com.project.webhook_service.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;

    @Transactional
    public PartnerResponse registerPartner(PartnerRegistrationRequest request) {
        Partner partner = partnerRepository.findByPartnerId(request.getPartnerId())
                .map(existing -> {
                    existing.setWebhookUrl(request.getWebhookUrl());
                    existing.setActive(true);
                    return existing;
                })
                .orElseGet(() -> {
                    return Partner.builder()
                            .partnerId(request.getPartnerId())
                            .webhookUrl(request.getWebhookUrl())
                            .active(true)
                            .build();
                });

        Partner saved = partnerRepository.save(partner);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PartnerResponse getPartner(String partnerId) {
        Partner partner = partnerRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId));
        return toResponse(partner);
    }

    @Transactional(readOnly = true)
    public List<PartnerResponse> listPartners() {
        return partnerRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Partner getPartnerEntity(String partnerId) {
        return partnerRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new PartnerNotFoundException(partnerId));
    }

    private PartnerResponse toResponse(Partner partner) {
        return PartnerResponse.builder()
                .id(partner.getId())
                .partnerId(partner.getPartnerId())
                .webhookUrl(partner.getWebhookUrl())
                .active(partner.getActive())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }
}
