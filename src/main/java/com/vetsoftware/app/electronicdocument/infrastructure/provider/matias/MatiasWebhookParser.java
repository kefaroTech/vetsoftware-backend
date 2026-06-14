package com.vetsoftware.app.electronicdocument.infrastructure.provider.matias;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetsoftware.app.electronicdocument.application.port.out.ParsedWebhook;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderWebhookParser;
import com.vetsoftware.app.electronicdocument.domain.WebhookOutcome;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Webhook de MATIAS: payload {@code { id, event, created_at, data:{...} }} con firma HMAC-SHA256 en el
 * header {@code X-Webhook-Signature}. Eventos de interés: {@code document.accepted} / {@code document.rejected}.
 * NOTA(schema): nombres de campos de {@code data} según doc pública; confirmar contra MATIAS real (TODO).
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
            String key = firstNonNull(text(data, "document_key"), text(data, "id"), text(data, "uuid"));
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
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private static String text(JsonNode node, String key) {
        if (node == null) return null;
        JsonNode value = node.get(key);
        return value == null || value.isNull() ? null : value.asText();
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
