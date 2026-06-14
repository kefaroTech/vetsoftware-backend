package com.vetsoftware.app.electronicdocument.infrastructure.provider.matias;

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
 * Adaptador MATIAS (proveedor ASÍNCRONO): `POST /invoice` deja el documento en cola y devuelve un
 * `document_key`; la validación DIAN llega después por webhook (ver MatiasWebhookParser /
 * DianWebhookController). Auth por Personal Access Token (config.apiToken), header Bearer.
 *
 * Listo para usarse cambiando la config de la empresa a provider=MATIAS. NOTA: los nombres de campos
 * del request siguen la doc pública de MATIAS y deben CONFIRMARSE contra su Colección Postman (TODO).
 */
@Component
public class MatiasInvoiceProvider implements ElectronicInvoiceProviderPort {

    private final RestClient restClient;

    public MatiasInvoiceProvider(@Qualifier("dianRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String providerName() {
        return "MATIAS";
    }

    @Override
    public ProviderResult transmit(ElectronicDocument document, ProviderConfigSnapshot config) {
        try {
            Map<String, Object> body = buildInvoiceRequest(document, config);
            JsonNode response = restClient.post()
                    .uri(base(config) + "/invoice")
                    .header("Authorization", "Bearer " + config.apiToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String documentKey = firstNonNull(
                    text(response, "document_key"),
                    text(at(response, "data"), "document_key"),
                    text(at(response, "data"), "id"));
            // Async: el documento queda PENDIENTE; el webhook lo cerrará como VALIDADO/RECHAZADO.
            return new ProviderResult(DianStatus.PENDIENTE, null, null, null, null, null, null, null, null,
                    null, documentKey, null, null, 202, safe(response));
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            if (status.is5xxServerError()) {
                return contingency(status.value(), responseBody);
            }
            return rejected(status.value(), responseBody);
        } catch (ResourceAccessException e) {
            return contingency(null, e.getMessage());
        }
    }

    // TODO(schema): confirmar nombres/estructura contra la doc/Postman de MATIAS.
    private Map<String, Object> buildInvoiceRequest(ElectronicDocument doc, ProviderConfigSnapshot config) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type_document_id", typeDocumentId(doc.getDocumentType()));
        body.put("reference_code", "EDOC-" + doc.getId());
        body.put("payment_form", doc.getPaymentForm() == PaymentForm.CREDITO ? "2" : "1");
        body.put("payment_method_code", "10"); // TODO mapear desde los pagos

        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("identification_number", doc.getCustomer().documentId());
        customer.put("name", doc.getCustomer().name());
        if (doc.getCustomer().legalName() != null) {
            customer.put("company_name", doc.getCustomer().legalName());
        }
        body.put("customer", customer);

        List<Map<String, Object>> items = new ArrayList<>();
        for (ElectronicDocumentLine line : doc.getLines()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", String.valueOf(line.getLineNumber()));
            item.put("description", line.getDescription());
            item.put("quantity", line.getQuantity());
            item.put("price", line.getLineExtensionAmount());
            item.put("tax_rate", line.getTaxRate() == null ? 0 : line.getTaxRate());
            item.put("unit_measure_code", line.getUnitMeasureCode());
            item.put("is_excluded", line.getTaxScheme() == null ? 1 : 0);
            items.add(item);
        }
        body.put("items", items);

        // Nota credito/debito: referencia a la factura corregida (billing_reference) + concepto de
        // correccion (discrepancy_response). TODO(schema): confirmar nombres contra la Postman de MATIAS.
        if (doc.isNote()) {
            DocumentReference ref = doc.getReference();
            if (ref != null) {
                Map<String, Object> billingReference = new LinkedHashMap<>();
                billingReference.put("cufe", ref.cufe());
                billingReference.put("prefix", ref.prefix());
                billingReference.put("number", ref.number());
                billingReference.put("issue_date", ref.issueDate() == null ? null : ref.issueDate().toString());
                body.put("billing_reference", billingReference);
            }
            Map<String, Object> discrepancy = new LinkedHashMap<>();
            discrepancy.put("correction_concept_code", doc.getNoteReasonCode());
            discrepancy.put("description", doc.getNoteReasonText());
            body.put("discrepancy_response", discrepancy);
        }
        return body;
    }

    /** Tipo de documento DIAN/MATIAS: FE venta=01, doc. equiv. POS=20, NC=05, ND=04 (TODO confirmar). */
    private static String typeDocumentId(ElectronicDocumentType type) {
        return switch (type) {
            case FE_VENTA -> "01";
            case DOC_EQUIV_POS -> "20";
            case NOTA_CREDITO -> "05";
            case NOTA_DEBITO -> "04";
        };
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

    private static JsonNode at(JsonNode root, String key) {
        return root == null ? null : root.get(key);
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

    private static String safe(JsonNode node) {
        if (node == null) return null;
        String s = node.toString();
        return s.substring(0, Math.min(s.length(), 2000));
    }
}
