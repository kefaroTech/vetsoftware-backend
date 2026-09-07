package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vetsoftware.app.paymentgateway.application.port.out.WompiEventPort;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.ParsedWompiEvent;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.WompiMalformedEventException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Interpreta el JSON crudo del webhook y calcula su checksum.
 *
 * <p>
 * <strong>El checksum se calcula sobre los valores tal como el JSON los
 * trae</strong> —no sobre una reserialización propia— porque
 * {@code signature.properties} nombra rutas dentro de {@code data}
 * ({@code "transaction.id"}, {@code "transaction.status"}, …) que este
 * adaptador resuelve nodo a nodo.
 */
@Component
public class WompiEventJsonAdapter implements WompiEventPort {

    private final ObjectMapper objectMapper;
    private final WompiProperties properties;

    public WompiEventJsonAdapter(ObjectMapper objectMapper, WompiProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public ParsedWompiEvent parse(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String eventType = textOrNull(root.path("event"));
            JsonNode transaction = root.path("data").path("transaction");
            String transactionId = textOrNull(transaction.path("id"));
            String rawStatus = textOrNull(transaction.path("status"));
            JsonNode timestampNode = root.path("timestamp");
            if (eventType == null || transaction.isMissingNode() || transactionId == null
                    || rawStatus == null || timestampNode.isMissingNode()) {
                throw new WompiMalformedEventException(
                        "El webhook de Wompi no trae los campos obligatorios", null);
            }
            long timestamp = timestampNode.asLong();
            long amountInCents = transaction.path("amount_in_cents").asLong();
            List<String> propertyValues = new ArrayList<>();
            for (JsonNode property : root.path("signature").path("properties")) {
                propertyValues.add(resolve(root.path("data"), property.asText()));
            }
            GatewayTransactionStatus status = GatewayTransactionStatus.valueOf(rawStatus);
            return new ParsedWompiEvent(eventType, transactionId, status,
                    textOrNull(transaction.path("status_message")), timestamp, amountInCents,
                    propertyValues);
        } catch (WompiMalformedEventException e) {
            throw e;
        } catch (Exception e) {
            throw new WompiMalformedEventException("No se pudo interpretar el webhook de Wompi", e);
        }
    }

    @Override
    public boolean matchesChecksum(ParsedWompiEvent event, String checksumHeader) {
        return WompiSignatures.matchesEventChecksum(checksumHeader, event.checksumPropertyValues(),
                event.timestamp(), properties.eventsSecret());
    }

    @Override
    public String computeChecksum(ParsedWompiEvent event) {
        return WompiSignatures.eventChecksum(event.checksumPropertyValues(), event.timestamp(),
                properties.eventsSecret());
    }

    @Override
    public void requireConfigured() {
        if (!properties.enabled()) {
            throw new PaymentGatewayNotConfiguredException(
                    "Wompi no está habilitado (vetsoftware.payments.wompi.enabled=false)");
        }
        if (properties.eventsSecret().isBlank()) {
            throw new PaymentGatewayNotConfiguredException(
                    "Wompi no tiene configurado el secreto de eventos"
                            + " (vetsoftware.payments.wompi.events-secret)");
        }
    }

    @Override
    public Duration freshnessTolerance() {
        return properties.eventFreshnessTolerance();
    }

    private static String textOrNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    /**
     * Resuelve una ruta con puntos ({@code "transaction.id"}) dentro de
     * {@code data}.
     */
    private static String resolve(JsonNode data, String dottedPath) {
        JsonNode node = data;
        for (String segment : dottedPath.split("\\.")) {
            node = node.path(segment);
        }
        return node.isMissingNode() || node.isNull() ? "" : node.asText();
    }
}
