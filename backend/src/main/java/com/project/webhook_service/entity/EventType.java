package com.project.webhook_service.entity;

public enum EventType {
    KYC_REGISTERED,
    TXN_SCREENED,
    TXN_BLOCKED,
    TXN_RELEASED,
    INVALID_TXN
}
