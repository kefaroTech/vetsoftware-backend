package com.vetsoftware.app.electronicdocument.infrastructure.provider.factus;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicInvoiceProviderPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderResult;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.DocumentReference;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adaptador Factus (proveedor SÍNCRONO): valida en `POST /v1/bills/validate` y devuelve el resultado
 * DIAN en la misma respuesta. Auth OAuth2 password en `/oauth/token`.
 *
 * NOTA: los nombres de campos del request siguen la documentación pública de Factus y deben CONFIRMARSE
 * contra la Colección Postman / sandbox real antes de producción (marcados con TODO). El request se arma
 * como Map snake_case (Factus usa snake_case) y la respuesta se parsea defensivamente como JsonNode para
 * tolerar diferencias de esquema.
 */
@Component
public class FactusInvoiceProvider implements ElectronicInvoiceProviderPort {

    private final RestClient restClient;

    public FactusInvoiceProvider(@Qualifier("dianRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String providerName() {
        return "FACTUS";
    }

    @Override
    public ProviderResult transmit(ElectronicDocument document, ProviderConfigSnapshot config) {
        try {
            String token = authenticate(config);
            Map<String, Object> body = buildBillRequest(document, config);
            JsonNode response = restClient.post()
                    .uri(base(config) + endpoint(document.getDocumentType()))
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return parseBill(response);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            if (status.is5xxServerError()) {
                // Indisponibilidad del proveedor/DIAN -> contingencia (reintentar luego).
                return contingency(status.value(), responseBody);
            }
            // 4xx: rechazo / validación fallida.
            return rejected(status.value(), responseBody);
        } catch (ResourceAccessException e) {
            // Timeout / conexión: contingencia.
            return contingency(null, e.getMessage());
        }
    }

    /** OAuth2 password grant: POST /oauth/token. Token de 1h (aquí se obtiene por transmisión). */
    private String authenticate(ProviderConfigSnapshot config) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", config.clientId());
        form.put("client_secret", config.clientSecret());
        form.put("username", config.username());
        form.put("password", config.password());
        JsonNode response = restClient.post()
                .uri(base(config) + "/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        String token = text(response, "access_token");
        if (token == null) {
            throw new IllegalStateException("Factus no devolvió access_token");
        }
        return token;
    }

    // TODO(schema): confirmar nombres exactos contra la doc/Postman de Factus.
    private Map<String, Object> buildBillRequest(ElectronicDocument doc, ProviderConfigSnapshot config) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (config.numberingProviderRef() != null && !config.numberingProviderRef().isBlank()) {
            body.put("numbering_range_id", config.numberingProviderRef());
        }
        body.put("reference_code", "EDOC-" + doc.getId());
        body.put("observation", "");
        body.put("payment_form", doc.getPaymentForm() == PaymentForm.CREDITO ? "2" : "1");
        body.put("payment_method_code", "10"); // TODO mapear desde los pagos del documento

        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("identification", doc.getCustomer().documentId());
        customer.put("names", doc.getCustomer().name());
        if (doc.getCustomer().legalName() != null) {
            customer.put("company", doc.getCustomer().legalName());
        }
        body.put("customer", customer);

        List<Map<String, Object>> items = new ArrayList<>();
        for (ElectronicDocumentLine line : doc.getLines()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code_reference", String.valueOf(line.getLineNumber()));
            item.put("name", line.getDescription());
            item.put("quantity", line.getQuantity());
            item.put("discount_rate", 0);
            item.put("price", line.getLineExtensionAmount()); // base sin IVA
            item.put("tax_rate", line.getTaxRate() == null ? 0 : line.getTaxRate());
            item.put("unit_measure_id", line.getUnitMeasureCode());
            item.put("is_excluded", line.getTaxScheme() == null ? 1 : 0);
            items.add(item);
        }
        body.put("items", items);

        // Nota credito/debito: referencia a la factura corregida + concepto DIAN. TODO(schema): confirmar
        // nombres exactos del bloque de referencia contra la doc/Postman de Factus para notas.
        if (doc.isNote()) {
            DocumentReference ref = doc.getReference();
            Map<String, Object> billingReference = new LinkedHashMap<>();
            if (ref != null) {
                billingReference.put("cufe", ref.cufe());
                billingReference.put("number", (ref.prefix() == null ? "" : ref.prefix())
                        + (ref.number() == null ? "" : ref.number()));
                billingReference.put("issue_date", ref.issueDate() == null ? null : ref.issueDate().toString());
            }
            body.put("billing_reference", billingReference);
            body.put("correction_concept_code", doc.getNoteReasonCode());
        }
        return body;
    }

    /** Endpoint Factus por tipo: factura vs notas. TODO(schema): confirmar rutas reales de notas. */
    private static String endpoint(ElectronicDocumentType type) {
        return switch (type) {
            case NOTA_CREDITO -> "/v1/credit-notes/validate";
            case NOTA_DEBITO -> "/v1/debit-notes/validate";
            default -> "/v1/bills/validate";
        };
    }

    /** Parseo defensivo de la respuesta síncrona: cufe presente => VALIDADO. */
    private ProviderResult parseBill(JsonNode response) {
        JsonNode bill = at(response, "data", "bill");
        if (bill == null) bill = at(response, "data");
        if (bill == null) bill = response;

        String cufe = text(bill, "cufe");
        String number = text(bill, "number");
        String prefix = text(bill, "prefix");
        String qrUrl = firstNonNull(text(bill, "qr"), text(bill, "public_url"), text(bill, "qr_image"));
        Long consecutive = parseLong(firstNonNull(text(bill, "consecutive"), number));

        if (cufe != null && !cufe.isBlank()) {
            return new ProviderResult(DianStatus.VALIDADO, prefix, consecutive, cufe, null,
                    text(bill, "uuid"), null, null, qrUrl, text(bill, "pdf_url"),
                    number, null, LocalDateTime.now(), 200, safe(response));
        }
        // Sin CUFE => tratar como rechazo (la DIAN no validó).
        return new ProviderResult(DianStatus.RECHAZADO, null, null, null, null, null, null, null, null, null,
                number, text(bill, "message"), null, 200, safe(response));
    }

    private ProviderResult rejected(int httpStatus, String body) {
        return new ProviderResult(DianStatus.RECHAZADO, null, null, null, null, null, null, null, null, null,
                null, body, null, httpStatus, body);
    }

    private ProviderResult contingency(Integer httpStatus, String body) {
        return new ProviderResult(DianStatus.CONTINGENCIA, null, null, null, null, null, null, null, null, null,
                null, body, null, httpStatus, body);
    }

    private static String base(ProviderConfigSnapshot config) {
        String url = config.baseUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static JsonNode at(JsonNode root, String... path) {
        JsonNode node = root;
        for (String key : path) {
            if (node == null) return null;
            node = node.get(key);
        }
        return node;
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

    private static String safe(JsonNode node) {
        if (node == null) return null;
        String s = node.toString();
        return s.substring(0, Math.min(s.length(), 2000));
    }
}
