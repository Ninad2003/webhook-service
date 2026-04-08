package com.project.webhook_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "delivery_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_event_id", nullable = false)
    private WebhookEvent webhookEvent;

    @Column(nullable = false)
    private int attemptNumber;

    /** HTTP response status code. Null if the request failed before getting a response. */
    @Column
    private Integer statusCode;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    /** Round-trip time in milliseconds. */
    @Column
    private Long responseTimeMs;

    /** Error message if the request failed (connection timeout, DNS failure, etc.) */
    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
