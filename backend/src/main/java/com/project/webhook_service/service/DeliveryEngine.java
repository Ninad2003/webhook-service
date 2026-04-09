package com.project.webhook_service.service;

import com.project.webhook_service.entity.DeliveryAttempt;
import com.project.webhook_service.entity.DeliveryStatus;
import com.project.webhook_service.entity.Partner;
import com.project.webhook_service.entity.WebhookEvent;
import com.project.webhook_service.repository.DeliveryAttemptRepository;
import com.project.webhook_service.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryEngine {

    private final WebhookEventRepository webhookEventRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final PartnerService partnerService;
    private final RestTemplate restTemplate;

    private static final long[] BACKOFF_SECONDS = {0, 10, 30, 120, 600};
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void recoverFromCrash() {
        try {
            webhookEventRepository.resetStaleProcessingEvents(Instant.now());
        } catch (Exception e) {
        }
    }

    @Scheduled(fixedDelay = 200)
    public void pollAndDeliver() {
        Instant now = Instant.now();
        List<String> partnerIds = webhookEventRepository.findPartnersWithDeliverableEvents(now);

        if (partnerIds.isEmpty()) {
            return;
        }

        for (String partnerId : partnerIds) {
            webhookEventRepository.findNextDeliverableEvent(partnerId, now)
                    .ifPresent(this::deliverEvent);
        }
    }

    @Transactional
    public void deliverEvent(WebhookEvent event) {
        event.setStatus(DeliveryStatus.PROCESSING);
        webhookEventRepository.save(event);

        Partner partner;
        try {
            partner = partnerService.getPartnerEntity(event.getPartnerId());
        } catch (Exception e) {
            event.setStatus(DeliveryStatus.FAILED);
            webhookEventRepository.save(event);
            return;
        }

        if (!partner.getActive()) {
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
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            statusCode = e.getStatusCode().value();
            responseBody = truncate(e.getResponseBodyAsString(), 1000);
            error = e.getMessage();
        } catch (ResourceAccessException e) {
            error = "Connection failed: " + e.getMessage();
        } catch (Exception e) {
            error = "Unexpected error: " + e.getMessage();
        }

        long responseTimeMs = System.currentTimeMillis() - startTime;

        DeliveryAttempt attempt = DeliveryAttempt.builder()
                .webhookEvent(event)
                .attemptNumber(attemptNumber)
                .statusCode(statusCode)
                .responseBody(responseBody)
                .responseTimeMs(responseTimeMs)
                .error(error)
                .build();
        deliveryAttemptRepository.save(attempt);

        event.setAttemptCount(attemptNumber);

        if (success) {
            event.setStatus(DeliveryStatus.DELIVERED);
        } else if (attemptNumber >= event.getMaxAttempts()) {
            event.setStatus(DeliveryStatus.FAILED);
        } else {
            long backoffSeconds = getBackoffSeconds(attemptNumber);
            event.setStatus(DeliveryStatus.PENDING);
            event.setNextRetryAt(Instant.now().plusSeconds(backoffSeconds));
        }

        webhookEventRepository.save(event);
    }

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
