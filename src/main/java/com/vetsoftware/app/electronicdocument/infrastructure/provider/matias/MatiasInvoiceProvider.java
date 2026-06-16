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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adaptador MATIAS (UBL 2.1). Modelo de la API oficial (Colección Postman sandbox):
 * <ul>
 *   <li>Auth: {@code POST /auth/login} con {@code {email,password}} → {@code access_token}; el token se
 *       cachea en memoria por empresa. Si la config trae un PAT pre-generado ({@code apiToken}) se usa ese
 *       y se omite el login.</li>
 *   <li>Emisión por tipo: factura {@code POST /invoice}, doc. equivalente POS
 *       {@code POST /auto-increment/pos-documents}, nota crédito {@code POST /notes/credit}, nota débito
 *       {@code POST /notes/debit}.</li>
 *   <li>Estado: {@code POST /status/document/{trackId}} (respaldo de reconciliación; el cierre principal
 *       llega por webhook).</li>
 * </ul>
 * El {@code baseUrl} de la config debe incluir el sufijo de versión, p. ej.
 * {@code https://sandbox-api.matias-api.com/api/ubl2.1}.
 *
 * <p><b>PROVISIONAL:</b> los IDs de catálogo de MATIAS (city_id, tax_id, identity_document_id, …) se toman
 * de los valores de ejemplo del Postman (constantes {@code EX_*}); deben mapearse desde el dominio antes de
 * producción. Marcado con {@code TODO(catalog)}.
 */
@Component
public class MatiasInvoiceProvider implements ElectronicInvoiceProviderPort {

    // --- Valores de catálogo MATIAS PROVISIONALES (Colección Postman sandbox). TODO(catalog): mapear desde el dominio. ---
    private static final String EX_RESOLUTION_NUMBER = "18764074347312";
    private static final String EX_PREFIX = "SETP";
    private static final int EX_OPERATION_TYPE_ID = 1;
    private static final String EX_COUNTRY_ID = "45";
    private static final String EX_CITY_ID = "836";
    private static final String EX_IDENTITY_DOCUMENT_ID = "1"; // tipo de documento del adquiriente (ej. CC)
    private static final int EX_TYPE_ORGANIZATION_ID = 2;      // 2 = persona natural (ej.)
    private static final int EX_TAX_REGIME_ID = 2;
    private static final int EX_TAX_LEVEL_ID = 5;
    private static final String EX_QUANTITY_UNITS_ID = "1093";
    private static final String EX_TYPE_ITEM_IDENT_ID = "4";
    private static final String EX_REFERENCE_PRICE_ID = "1";
    private static final String EX_TAX_ID = "1";              // IVA
    private static final int EX_MEANS_PAYMENT_ID = 10;        // 10 = efectivo
    private static final String EX_NC_RESPONSE_ID = "2";      // concepto de corrección de la nota (ej.)

    /** TTL conservador del token de login mientras MATIAS no documente la expiración real. TODO: confirmar. */
    private static final long TOKEN_TTL_SECONDS = 50 * 60L;

    private final RestClient restClient;
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

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
            String token = authenticate(config);
            Map<String, Object> body = buildRequest(document, config);
            JsonNode response = restClient.post()
                    .uri(base(config) + endpoint(document.getDocumentType()))
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String trackId = extractTrackId(response);
            // El cierre VALIDADO/RECHAZADO llega por webhook (o por polling de /status como respaldo).
            return new ProviderResult(DianStatus.PENDIENTE, null, null, null, null, null, null, null, null,
                    null, trackId, null, null, 202, safe(response));
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

    /**
     * Reconciliación por polling: consulta {@code POST /status/document/{trackId}} cuando el webhook no
     * llegó. Ante una respuesta no concluyente (4xx/5xx/timeout) devuelve PENDIENTE para reintentar, nunca
     * un terminal especulativo.
     */
    @Override
    public Optional<ProviderResult> fetchStatus(String trackId, ProviderConfigSnapshot config) {
        if (trackId == null || trackId.isBlank()) return Optional.empty();
        try {
            String token = authenticate(config);
            JsonNode response = restClient.post()
                    .uri(base(config) + "/status/document/" + trackId)
                    .header("Authorization", "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            return Optional.of(parseStatus(response, trackId));
        } catch (RestClientResponseException | ResourceAccessException e) {
            return Optional.of(pending(trackId));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Auth
    // ---------------------------------------------------------------------------------------------

    /** Devuelve un Bearer válido: PAT pre-generado si la config lo trae, si no login con cache por empresa. */
    private String authenticate(ProviderConfigSnapshot config) {
        if (config.apiToken() != null && !config.apiToken().isBlank()) {
            return config.apiToken();
        }
        String cacheKey = base(config) + "|" + config.username();
        CachedToken cached = tokenCache.get(cacheKey);
        if (cached != null && cached.isFresh()) {
            return cached.token();
        }
        Map<String, Object> credentials = new LinkedHashMap<>();
        credentials.put("email", config.username());
        credentials.put("password", config.password());
        JsonNode response = restClient.post()
                .uri(base(config) + "/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(credentials)
                .retrieve()
                .body(JsonNode.class);
        String token = firstNonNull(text(response, "access_token"), text(response, "token"),
                text(at(response, "data"), "access_token"), text(at(response, "data"), "token"));
        if (token == null) {
            throw new IllegalStateException("MATIAS no devolvió access_token en /auth/login");
        }
        tokenCache.put(cacheKey, new CachedToken(token, LocalDateTime.now().plusSeconds(TOKEN_TTL_SECONDS)));
        return token;
    }

    private record CachedToken(String token, LocalDateTime expiresAt) {
        boolean isFresh() {
            return LocalDateTime.now().isBefore(expiresAt);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Routing + request building
    // ---------------------------------------------------------------------------------------------

    /** Endpoint MATIAS por tipo de documento. */
    private static String endpoint(ElectronicDocumentType type) {
        return switch (type) {
            case FE_VENTA -> "/invoice";
            case DOC_EQUIV_POS -> "/auto-increment/pos-documents";
            case NOTA_CREDITO -> "/notes/credit";
            case NOTA_DEBITO -> "/notes/debit";
        };
    }

    /** type_document_id MATIAS (de los ejemplos del Postman). TODO(catalog): confirmar códigos reales. */
    private static int typeDocumentId(ElectronicDocumentType type) {
        return switch (type) {
            case FE_VENTA -> 7;
            case DOC_EQUIV_POS -> 20;
            case NOTA_CREDITO -> 5;
            case NOTA_DEBITO -> 4;
        };
    }

    private Map<String, Object> buildRequest(ElectronicDocument doc, ProviderConfigSnapshot config) {
        Map<String, Object> body = new LinkedHashMap<>();
        // TODO: resolution_number/prefix/document_number reales vienen de la NumberingResolution de F1.
        body.put("resolution_number", EX_RESOLUTION_NUMBER);
        body.put("prefix", doc.getPrefix() != null ? doc.getPrefix() : EX_PREFIX);
        body.put("document_number", String.valueOf(doc.getConsecutive() != null ? doc.getConsecutive() : doc.getId()));
        body.put("notes", "Documento " + doc.getId());
        body.put("operation_type_id", EX_OPERATION_TYPE_ID);
        body.put("type_document_id", typeDocumentId(doc.getDocumentType()));
        body.put("graphic_representation", 0);
        body.put("send_email", 0); // la representación/correo la genera el sistema (Gotenberg), no MATIAS.
        body.put("payments", buildPayments(doc));
        body.put("customer", buildCustomer(doc));
        body.put("lines", buildLines(doc));
        body.put("legal_monetary_totals", buildMonetaryTotals(doc));
        body.put("tax_totals", buildTaxTotals(doc));

        if (doc.getDocumentType() == ElectronicDocumentType.DOC_EQUIV_POS) {
            body.put("document_signature", buildDocumentSignature());
            body.put("point_of_sale", buildPointOfSale());
            body.put("software_manufacturer", buildSoftwareManufacturer());
        }

        // Nota crédito/débito: referencia a la factura corregida + concepto de corrección.
        if (doc.isNote()) {
            DocumentReference ref = doc.getReference();
            if (ref != null) {
                Map<String, Object> billingReference = new LinkedHashMap<>();
                billingReference.put("number", ref.number());
                billingReference.put("date", ref.issueDate() == null ? null : ref.issueDate().toString());
                billingReference.put("uuid", ref.cufe());
                body.put("billing_reference", billingReference);

                Map<String, Object> discrepancy = new LinkedHashMap<>();
                discrepancy.put("reference_id", String.valueOf(ref.number()));
                // response_id = concepto DIAN de la corrección; usamos el código de la nota si lo hay.
                discrepancy.put("response_id",
                        doc.getNoteReasonCode() != null ? doc.getNoteReasonCode() : EX_NC_RESPONSE_ID);
                body.put("discrepancy_response", discrepancy);
            }
        }
        return body;
    }

    private List<Map<String, Object>> buildPayments(ElectronicDocument doc) {
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("payment_method_id", doc.getPaymentForm() == PaymentForm.CREDITO ? 2 : 1);
        payment.put("means_payment_id", parseIntOr(doc.primaryPaymentMeansCode(), EX_MEANS_PAYMENT_ID));
        payment.put("value_paid", str(doc.getPayableAmount()));
        return List.of(payment);
    }

    private Map<String, Object> buildCustomer(ElectronicDocument doc) {
        Map<String, Object> customer = new LinkedHashMap<>();
        String companyName = doc.getCustomer().legalName() != null
                ? doc.getCustomer().legalName() : doc.getCustomer().name();
        customer.put("company_name", companyName);
        customer.put("dni", doc.getCustomer().documentId());
        if (doc.getCustomer().email() != null) {
            customer.put("email", doc.getCustomer().email());
        }
        // TODO(catalog): mapear estos IDs desde el dominio (City/tipo de documento/régimen). Provisionales:
        customer.put("country_id", EX_COUNTRY_ID);
        customer.put("city_id", EX_CITY_ID);
        customer.put("identity_document_id", EX_IDENTITY_DOCUMENT_ID);
        customer.put("type_organization_id", EX_TYPE_ORGANIZATION_ID);
        customer.put("tax_regime_id", EX_TAX_REGIME_ID);
        customer.put("tax_level_id", EX_TAX_LEVEL_ID);
        return customer;
    }

    private List<Map<String, Object>> buildLines(ElectronicDocument doc) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ElectronicDocumentLine line : doc.getLines()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("invoiced_quantity", str(line.getQuantity()));
            item.put("quantity_units_id", EX_QUANTITY_UNITS_ID);
            item.put("line_extension_amount", str(line.getLineExtensionAmount()));
            item.put("free_of_charge_indicator", false);
            item.put("description", line.getDescription());
            item.put("code", String.valueOf(line.getLineNumber()));
            item.put("type_item_identifications_id", EX_TYPE_ITEM_IDENT_ID);
            item.put("reference_price_id", EX_REFERENCE_PRICE_ID);
            item.put("price_amount", str(line.getUnitPrice()));
            item.put("base_quantity", str(line.getQuantity()));
            item.put("tax_totals", lineTaxTotals(line));
            items.add(item);
        }
        return items;
    }

    /** tax_totals de una línea (vacío si la línea no lleva impuesto). */
    private static List<Map<String, Object>> lineTaxTotals(ElectronicDocumentLine line) {
        if (line.getTaxRate() == null || line.getTaxRate().signum() <= 0) {
            return List.of();
        }
        Map<String, Object> tax = new LinkedHashMap<>();
        tax.put("tax_id", EX_TAX_ID);
        tax.put("tax_amount", line.getTaxAmount());
        tax.put("taxable_amount", line.getLineExtensionAmount());
        tax.put("percent", line.getTaxRate());
        return List.of(tax);
    }

    /** tax_totals del documento: agrega las líneas que llevan impuesto (mismo formato que las de línea). */
    private List<Map<String, Object>> buildTaxTotals(ElectronicDocument doc) {
        List<Map<String, Object>> totals = new ArrayList<>();
        for (ElectronicDocumentLine line : doc.getLines()) {
            totals.addAll(lineTaxTotals(line));
        }
        return totals;
    }

    private Map<String, Object> buildMonetaryTotals(ElectronicDocument doc) {
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("line_extension_amount", str(doc.getLineExtensionAmount()));
        totals.put("tax_exclusive_amount", str(doc.getTaxExclusiveAmount()));
        totals.put("tax_inclusive_amount", str(doc.getTaxInclusiveAmount()));
        totals.put("payable_amount", doc.getPayableAmount());
        return totals;
    }

    // Bloques POS — datos operativos que aún no modelamos; provisionales de ejemplo. TODO: parametrizar.
    private Map<String, Object> buildDocumentSignature() {
        Map<String, Object> signature = new LinkedHashMap<>();
        signature.put("cashier", "Cajero");
        signature.put("seller", "Vendedor");
        return signature;
    }

    private Map<String, Object> buildPointOfSale() {
        Map<String, Object> pos = new LinkedHashMap<>();
        pos.put("cashier_name", "Cajero");
        pos.put("terminal_number", "CJ001");
        pos.put("cashier_type", "Caja principal");
        pos.put("sales_code", "POS01");
        pos.put("address", "N/A");
        return pos;
    }

    private Map<String, Object> buildSoftwareManufacturer() {
        Map<String, Object> sw = new LinkedHashMap<>();
        sw.put("owner_name", "VetSoftware");
        sw.put("company_name", "VetSoftware");
        sw.put("software_name", "VetSoftware");
        return sw;
    }

    // ---------------------------------------------------------------------------------------------
    // Status parsing
    // ---------------------------------------------------------------------------------------------

    /** Mapea la respuesta de {@code /status/document/{trackId}} a un ProviderResult. TODO(schema): confirmar campos. */
    private ProviderResult parseStatus(JsonNode response, String trackId) {
        JsonNode data = at(response, "data");
        if (data == null) data = response;
        String status = firstNonNull(text(data, "status"), text(data, "dian_status"), text(response, "status"));
        String s = status == null ? "" : status.toLowerCase();
        if (s.contains("accept") || s.contains("valid") || s.contains("aprob")) {
            return new ProviderResult(DianStatus.VALIDADO, text(data, "prefix"),
                    parseLong(firstNonNull(text(data, "consecutive"), text(data, "number"))),
                    text(data, "cufe"), text(data, "cude"), text(data, "uuid"),
                    text(data, "xml"), text(data, "qr"),
                    firstNonNull(text(data, "qr_url"), text(data, "public_url")), text(data, "pdf_url"),
                    trackId, null, LocalDateTime.now(), 200, safe(response));
        }
        if (s.contains("reject") || s.contains("rechaz")) {
            return new ProviderResult(DianStatus.RECHAZADO, null, null, null, null, null, null, null, null, null,
                    trackId, firstNonNull(text(data, "message"), text(data, "error")),
                    null, 200, safe(response));
        }
        return pending(trackId);
    }

    private ProviderResult pending(String trackId) {
        return new ProviderResult(DianStatus.PENDIENTE, null, null, null, null, null, null, null, null, null,
                trackId, null, null, null, null);
    }

    private ProviderResult rejected(int httpStatus, String body) {
        return new ProviderResult(DianStatus.RECHAZADO, null, null, null, null, null, null, null, null, null,
                null, body, null, httpStatus, body);
    }

    private ProviderResult contingency(Integer httpStatus, String body) {
        return new ProviderResult(DianStatus.CONTINGENCIA, null, null, null, null, null, null, null, null, null,
                null, body, null, httpStatus, body);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static String extractTrackId(JsonNode response) {
        return firstNonNull(
                text(response, "trackId"), text(response, "track_id"),
                text(at(response, "data"), "trackId"), text(at(response, "data"), "track_id"),
                text(response, "document_key"), text(at(response, "data"), "id"),
                text(at(response, "data"), "cude"), text(at(response, "data"), "cufe"));
    }

    private static String base(ProviderConfigSnapshot config) {
        String url = config.baseUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String str(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static int parseIntOr(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
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
