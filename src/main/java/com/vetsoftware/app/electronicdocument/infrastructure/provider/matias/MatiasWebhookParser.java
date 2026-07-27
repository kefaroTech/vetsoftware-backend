package com.vetsoftware.app.electronicdocument.infrastructure.provider.matias;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vetsoftware.app.electronicdocument.application.port.out.ParsedWebhook;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderWebhookParser;
import com.vetsoftware.app.electronicdocument.domain.WebhookOutcome;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Webhook de MATIAS (docs: endpoints/webhooks). Payload:
 * {@code { id, event, created_at, data:{ document_id, track_id, document_type, customer_name, total, status } }}
 * con firma HMAC-SHA256 (sobre el JSON del payload) en el header {@code X-Webhook-Signature: sha256=...}
 * (además {@code X-Webhook-ID}, {@code X-Event-Type}). Eventos: {@code document.created/emitted/accepted/rejected/voided};
 * solo actuamos sobre {@code document.accepted} / {@code document.rejected}.
 *
 * <p>NOTA: el payload del webhook NO trae los sellos (CUFE/CUDE/XML/QR), solo {@code track_id} + {@code status}.
 * El cierre con CUFE viene de la respuesta SÍNCRONA de la emisión (HTTP 200); para el caso encolado, el CUFE
 * se obtiene del polling de estado ({@code MatiasInvoiceProvider.fetchStatus}). TODO: que el servicio de
 * webhook dispare un fetch de estado tras un {@code accepted} para poblar el CUFE.
 */
@Component
public class MatiasWebhookParser implements ProviderWebhookParser {

    private final ObjectMapper objectMapper;

    public MatiasWebhookParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return "MATIAS";
    }

    @Override
    public boolean verifySignature(String rawBody, String signatureHeader, String secret) {
        if (signatureHeader == null || secret == null) return false;
        String expected = hmacSha256Hex(rawBody, secret);
        String provided = signatureHeader.startsWith("sha256=")
                ? signatureHeader.substring("sha256=".length()) : signatureHeader;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public ParsedWebhook parse(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = text(root, "event");
            JsonNode data = root.get("data");
            WebhookOutcome outcome = outcomeOf(event);
            // El identificador real del webhook es track_id (coincide con el provider_document_key de la bitácora).
            String key = firstNonNull(text(data, "track_id"), text(data, "document_key"), text(data, "id"), text(data, "uuid"));
            return new ParsedWebhook(
                    outcome, key,
                    text(data, "prefix"), parseLong(firstNonNull(text(data, "consecutive"), text(data, "number"))),
                    text(data, "cufe"), text(data, "cude"), text(data, "uuid"),
                    text(data, "xml"), text(data, "qr"),
                    firstNonNull(text(data, "qr_url"), text(data, "public_url")),
                    text(data, "pdf_url"),
                    firstNonNull(text(data, "message"), text(data, "error")));
        } catch (Exception e) {
            throw new IllegalArgumentException("Webhook MATIAS inválido: " + e.getMessage(), e);
        }
    }

    private static WebhookOutcome outcomeOf(String event) {
        if (event == null) return WebhookOutcome.IGNORED;
        String e = event.toLowerCase();
        if (e.contains("accepted")) return WebhookOutcome.ACCEPTED;
        if (e.contains("rejected")) return WebhookOutcome.REJECTED;
        return WebhookOutcome.IGNORED;
    }

    private static String hmacSha256Hex(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private static String text(JsonNode node, String key) {
        if (node == null) return null;
        JsonNode value = node.get(key);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static String firstNonNull(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static Long parseLong(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
