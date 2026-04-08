package com.project.webhook_service.service;

import com.project.webhook_service.entity.DeliveryAttempt;
import com.project.webhook_service.entity.DeliveryStatus;
import com.project.webhook_service.entity.Partner;
import com.project.webhook_service.entity.WebhookEvent;
import com.project.webhook_service.repository.DeliveryAttemptRepository;
import com.project.webhook_service.repository.WebhookEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryEngine {

    private final WebhookEventRepository webhookEventRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final PartnerService partnerService;
    private final RestTemplate restTemplate;

    /** Backoff intervals in seconds: immediate, 10s, 30s, 120s, 600s */
    private static final long[] BACKOFF_SECONDS = {0, 10, 30, 120, 600};

    /**
     * On startup, reset any events stuck in PROCESSING state (from a previous crash)
     * back to PENDING so they can be retried.
     * Uses ApplicationReadyEvent to ensure tables are created by Hibernate before this runs.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void recoverFromCrash() {
        try {
            int reset = webhookEventRepository.resetStaleProcessingEvents(Instant.now());
            if (reset > 0) {
                log.warn("Crash recovery: Reset {} stale PROCESSING events back to PENDING", reset);
            } else {
                log.info("Crash recovery: No stale PROCESSING events found");
            }
        } catch (Exception e) {
            log.warn("Crash recovery check skipped (tables may not exist yet): {}", e.getMessage());
        }
    }

    /**
     * Scheduled polling task that runs every 200ms to pick up deliverable events.
     * For each partner with pending events, it picks the oldest (lowest sequence number)
     * event that is ready for delivery and processes it asynchronously.
     */
    @Scheduled(fixedDelay = 200)
    public void pollAndDeliver() {
        Instant now = Instant.now();
        List<String> partnerIds = webhookEventRepository.findPartnersWithDeliverableEvents(now);

        if (partnerIds.isEmpty()) {
            return;
        }

        log.debug("Found {} partners with deliverable events", partnerIds.size());

        for (String partnerId : partnerIds) {
            webhookEventRepository.findNextDeliverableEvent(partnerId, now)
                    .ifPresent(this::deliverAsync);
        }
    }

    /**
     * Asynchronously deliver a single event to the partner's webhook endpoint.
     * This runs on the custom async thread pool so multiple partners are served concurrently.
     */
    @Async("webhookDeliveryExecutor")
    public void deliverAsync(WebhookEvent event) {
        deliverEvent(event);
    }

    /**
     * Core delivery logic for a single event:
     * 1. Mark as PROCESSING
     * 2. POST to partner webhook URL
     * 3. Record delivery attempt
     * 4. Update status based on result
     */
    @Transactional
    public void deliverEvent(WebhookEvent event) {
        // Mark as PROCESSING
        event.setStatus(DeliveryStatus.PROCESSING);
        webhookEventRepository.save(event);

        Partner partner;
        try {
            partner = partnerService.getPartnerEntity(event.getPartnerId());
        } catch (Exception e) {
            log.error("Partner not found for event {}, marking as FAILED", event.getEventId());
            event.setStatus(DeliveryStatus.FAILED);
            webhookEventRepository.save(event);
            return;
        }

        if (!partner.getActive()) {
            log.warn("Partner {} is inactive, skipping delivery for event {}", partner.getPartnerId(), event.getEventId());
            event.setStatus(DeliveryStatus.PENDING);
            event.setNextRetryAt(Instant.now().plusSeconds(60));
            webhookEventRepository.save(event);
            return;
        }

        long startTime = System.currentTimeMillis();
        int attemptNumber = event.getAttemptCount() + 1;
        Integer statusCode = null;
        String responseBody = null;
        String error = null;
        boolean success = false;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Event-Id", event.getEventId());
            headers.set("X-Webhook-Event-Type", event.getEventType().name());
            headers.set("X-Webhook-Attempt", String.valueOf(attemptNumber));

            HttpEntity<String> httpEntity = new HttpEntity<>(event.getPayload(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    partner.getWebhookUrl(), httpEntity, String.class);

            statusCode = response.getStatusCode().value();
            responseBody = truncate(response.getBody(), 1000);
            success = response.getStatusCode().is2xxSuccessful();

            log.info("Delivery attempt {} for event {}: HTTP {} ({}ms)",
                    attemptNumber, event.getEventId(), statusCode, System.currentTimeMillis() - startTime);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            statusCode = e.getStatusCode().value();
            responseBody = truncate(e.getResponseBodyAsString(), 1000);
            error = e.getMessage();
            log.warn("Delivery attempt {} for event {}: HTTP {} - {}",
                    attemptNumber, event.getEventId(), statusCode, error);
        } catch (ResourceAccessException e) {
            error = "Connection failed: " + e.getMessage();
            log.warn("Delivery attempt {} for event {}: Connection error - {}",
                    attemptNumber, event.getEventId(), error);
        } catch (Exception e) {
            error = "Unexpected error: " + e.getMessage();
            log.error("Delivery attempt {} for event {}: Unexpected error",
                    attemptNumber, event.getEventId(), e);
        }

        long responseTimeMs = System.currentTimeMillis() - startTime;

        // Record the delivery attempt
        DeliveryAttempt attempt = DeliveryAttempt.builder()
                .webhookEvent(event)
                .attemptNumber(attemptNumber)
                .statusCode(statusCode)
                .responseBody(responseBody)
                .responseTimeMs(responseTimeMs)
                .error(error)
                .build();
        deliveryAttemptRepository.save(attempt);

        // Update event state
        event.setAttemptCount(attemptNumber);

        if (success) {
            event.setStatus(DeliveryStatus.DELIVERED);
            log.info("Event {} delivered successfully to partner {}", event.getEventId(), event.getPartnerId());
        } else if (attemptNumber >= event.getMaxAttempts()) {
            event.setStatus(DeliveryStatus.FAILED);
            log.warn("Event {} exhausted all {} attempts, marking as FAILED",
                    event.getEventId(), event.getMaxAttempts());
        } else {
            // Schedule retry with exponential backoff
            long backoffSeconds = getBackoffSeconds(attemptNumber);
            event.setStatus(DeliveryStatus.PENDING);
            event.setNextRetryAt(Instant.now().plusSeconds(backoffSeconds));
            log.info("Event {} scheduled for retry in {}s (attempt {}/{})",
                    event.getEventId(), backoffSeconds, attemptNumber, event.getMaxAttempts());
        }

        webhookEventRepository.save(event);
    }

    /**
     * Get backoff delay in seconds for a given attempt number.
     * Attempt 1: 0s (immediate first attempt)
     * Attempt 2: 10s
     * Attempt 3: 30s
     * Attempt 4: 120s (2 min)
     * Attempt 5+: 600s (10 min)
     */
    private long getBackoffSeconds(int attemptNumber) {
        if (attemptNumber <= 0) return 0;
        if (attemptNumber > BACKOFF_SECONDS.length) return BACKOFF_SECONDS[BACKOFF_SECONDS.length - 1];
        return BACKOFF_SECONDS[attemptNumber - 1];
    }

    private String truncate(String s, int maxLength) {
        if (s == null) return null;
        return s.length() <= maxLength ? s : s.substring(0, maxLength) + "...[truncated]";
    }
}
