package com.vetsoftware.app.paymentgateway.infrastructure.web.response;

import com.vetsoftware.app.paymentgateway.application.dto.WompiWebhookEventDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record WompiWebhookEventResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String gateway,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String eventType,
        String gatewayReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String eventChecksum,
        String processingOutcome,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime receivedAt,
        LocalDateTime processedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        String rawBody) {

    public static WompiWebhookEventResponse from(WompiWebhookEventDto dto) {
        return new WompiWebhookEventResponse(dto.id(), dto.gateway(), dto.eventType(),
                dto.gatewayReference(), dto.eventChecksum(), dto.processingOutcome(),
                dto.receivedAt(), dto.processedAt(), dto.createdDate(), dto.rawBody());
    }
}
