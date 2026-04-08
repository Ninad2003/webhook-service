package com.project.webhook_service.exception;

public class PartnerNotFoundException extends RuntimeException {
    public PartnerNotFoundException(String partnerId) {
        super("Partner not found: " + partnerId);
    }
}
