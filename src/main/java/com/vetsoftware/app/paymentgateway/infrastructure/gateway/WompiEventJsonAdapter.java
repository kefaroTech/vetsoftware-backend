package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vetsoftware.app.paymentgateway.application.port.out.WompiEventPort;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.ParsedWompiEvent;
import com.vetsoftware.app.paymentgateway.domain.WompiGatewayException;
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
            long timestamp = root.path("timestamp").asLong();
            List<String> propertyValues = new ArrayList<>();
            for (JsonNode property : root.path("signature").path("properties")) {
                propertyValues.add(resolve(root.path("data"), property.asText()));
            }
            String rawStatus = textOrNull(transaction.path("status"));
            GatewayTransactionStatus status = rawStatus == null
                    ? null
                    : GatewayTransactionStatus.valueOf(rawStatus);
            return new ParsedWompiEvent(eventType, textOrNull(transaction.path("id")), status,
                    textOrNull(transaction.path("status_message")), timestamp, propertyValues);
        } catch (Exception e) {
            throw new WompiGatewayException("No se pudo interpretar el webhook de Wompi", e);
        }
    }

    @Override
    public boolean matchesChecksum(ParsedWompiEvent event, String checksumHeader) {
        return WompiSignatures.matchesEventChecksum(checksumHeader, event.checksumPropertyValues(),
                event.timestamp(), properties.eventsSecret());
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
