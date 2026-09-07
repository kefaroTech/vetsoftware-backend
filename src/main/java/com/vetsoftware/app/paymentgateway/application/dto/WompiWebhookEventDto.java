package com.vetsoftware.app.paymentgateway.application.dto;

import java.time.LocalDateTime;

public record WompiWebhookEventDto(Long id, String gateway, String eventType,
        String gatewayReference, String eventChecksum, String processingOutcome,
        LocalDateTime receivedAt, LocalDateTime processedAt, LocalDateTime createdDate,
        String rawBody) {
}
