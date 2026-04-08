package com.project.webhook_service.controller;

import com.project.webhook_service.dto.PartnerRegistrationRequest;
import com.project.webhook_service.dto.PartnerResponse;
import com.project.webhook_service.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    /** Register or update a partner's webhook endpoint. */
    @PostMapping
    public ResponseEntity<PartnerResponse> registerPartner(@Valid @RequestBody PartnerRegistrationRequest request) {
        PartnerResponse response = partnerService.registerPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** List all registered partners. */
    @GetMapping
    public ResponseEntity<List<PartnerResponse>> listPartners() {
        return ResponseEntity.ok(partnerService.listPartners());
    }

    /** Get a specific partner by their business ID. */
    @GetMapping("/{partnerId}")
    public ResponseEntity<PartnerResponse> getPartner(@PathVariable String partnerId) {
        return ResponseEntity.ok(partnerService.getPartner(partnerId));
    }
}
