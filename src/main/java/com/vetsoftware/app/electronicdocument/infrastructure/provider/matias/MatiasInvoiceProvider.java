package com.vetsoftware.app.electronicdocument.infrastructure.provider.matias;

import com.fasterxml.jackson.databind.JsonNode;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicInvoiceProviderPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderResult;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.DocumentReference;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.FiscalResponsibility;
import com.vetsoftware.app.electronicdocument.domain.TaxRegime;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private static final Logger log = LoggerFactory.getLogger(MatiasInvoiceProvider.class);
    // Sello fiscal DIAN (CUFE/CUDE/CUDS) = SHA-384 → 96 caracteres hex.
    private static final java.util.regex.Pattern SEAL_SHA384 = java.util.regex.Pattern.compile("[0-9a-fA-F]{96}");

    // --- Catálogos MATIAS VERIFICADOS contra la API en vivo del sandbox (GET /document-type, /taxes, etc.). ---
    private static final int OPERATION_TYPE_NACIONAL = 1; // Operación: Estándar (GET /operation-type id 1)
    private static final String COUNTRY_COLOMBIA = "45";  // País: Colombia (GET /countries id 45)
    // El id de MATIAS cuyo "code" es el código DIAN/UNECE que guardamos en la línea (unit_measure_code). El XML
    // DIAN emite @unitCode = ese code, así que el id transmitido debe calzar con unit_measure_code. id 70 = code
    // "94" (unidad); el antiguo 1093 tenía code "ZZ" (mutuamente definido), que NO calzaba con el "94" guardado.
    private static final String UNIT_ID_UNIDAD = "70";    // code "94" = unidad (GET /quantity-units id 70)
    private static final Map<String, String> QUANTITY_UNIT_ID_BY_DIAN_CODE = Map.of(
            "94", UNIT_ID_UNIDAD
            // TODO(catalog): kg (KGM), litro (LTR), metro (MTR)… cuando se capture unidad por producto.
    );
    private static final int MEANS_PAYMENT_EFECTIVO = 10; // Medio de pago: Efectivo (GET /payment-means id 10) — fallback
    private static final String TAX_ID_IVA = "1";         // Impuesto: IVA (GET /taxes id 1, code 01)
    private static final String TAX_ID_INC = "4";         // Impuesto Nacional al Consumo (GET /taxes id 4, code 04)
    private static final String TAX_ID_RETEIVA = "5";     // Retención: ReteIVA (GET /taxes id 5, code 05)
    private static final String TAX_ID_RETEFUENTE = "6";  // Retención: ReteRenta/Fuente (GET /taxes id 6, code 06)
    private static final String TAX_ID_RETEICA = "7";     // Retención: ReteICA (GET /taxes id 7, code 07)
    private static final String ITEM_IDENT_CONTRIBUYENTE = "4"; // estándar del contribuyente (GET /type-item-identifications id 4)

    // --- Defaults válidos pero no específicos del adquiriente (confirmados emitiendo en el sandbox). ---
    private static final String EX_RESOLUTION_NUMBER = "18764074347312"; // TODO(F1): de la NumberingResolution de la empresa
    private static final String EX_PREFIX = "SETP";                      // TODO(F1): de la NumberingResolution de la empresa
    private static final String EX_CITY_ID = "959";          // default = ciudad de la empresa; TODO: city del adquiriente vía /cities (city_code=DANE)
    // tax_regime_id MATIAS (GET /taxes-regime): 1 = Responsable de IVA, 2 = No responsable de IVA.
    private static final int TAX_REGIME_RESPONSABLE_IVA = 1;
    private static final int TAX_REGIME_NO_RESPONSABLE_IVA = 2; // default/fallback (GET /company usa 2)
    // tax_level_id MATIAS (catálogo type_liabilities = responsabilidad fiscal / TaxLevelCode del adquiriente).
    // NO_APLICA (R-99-PN) = 5 es el default validado e2e (GET /company usa 5). El sandbox NO expone el GET del
    // catálogo de tax-levels, así que 7/9/14/112 (apidian) están PENDIENTES de confirmar emitiendo antes de
    // depender de ellos; solo se usan si el adquiriente declara una responsabilidad distinta de NO_APLICA.
    private static final int TAX_LEVEL_NO_APLICA = 5;             // R-99-PN (default; documentos sin dato → este)
    private static final int TAX_LEVEL_GRAN_CONTRIBUYENTE = 7;    // O-13  (TODO: confirmar contra catálogo vivo)
    private static final int TAX_LEVEL_AUTORRETENEDOR = 9;        // O-15  (TODO: confirmar)
    private static final int TAX_LEVEL_AGENTE_RETENCION_IVA = 14; // O-23  (TODO: confirmar)
    private static final int TAX_LEVEL_REGIMEN_SIMPLE = 112;      // O-47  (TODO: confirmar)
    private static final String EX_REFERENCE_PRICE_ID = "1"; // precio de referencia estándar (validado en el sandbox)
    private static final String EX_NC_RESPONSE_ID = "2";     // concepto de corrección por defecto (2=Anulación); ideal: mapear desde noteReasonCode

    /** Fallback de TTL del token si el login no devuelve {@code expires_at} (normalmente sí lo hace). */
    private static final long TOKEN_TTL_SECONDS = 50 * 60L;

    private final RestClient restClient;
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
    // Cache del catálogo de ciudades por base URL: DANE (city_code 5 díg.) → city_id interno de MATIAS.
    private final Map<String, Map<String, String>> cityCacheByBase = new ConcurrentHashMap<>();

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
            ResponseEntity<JsonNode> response = restClient.post()
                    .uri(base(config) + endpoint(document.getDocumentType()))
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(JsonNode.class);
            // MATIAS puede validar SÍNCRONO (HTTP 200, StatusCode 00) o encolar (HTTP 201) → webhook/polling.
            return parseDocumentResult(response.getBody(), document, response.getStatusCode().value(), null);
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            if (status.is5xxServerError()) {
                return contingency(status.value(), responseBody); // DIAN caída (500/503/504) → reintentar
            }
            return rejected(status.value(), responseBody);         // 4xx: 422 validación, 400 duplicado
        } catch (ResourceAccessException e) {
            return contingency(null, e.getMessage());              // timeout/conexión
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
            ResponseEntity<JsonNode> response = restClient.post()
                    .uri(base(config) + "/status/document/" + trackId)
                    .header("Authorization", "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(JsonNode.class);
            // En reconciliación el sello es el trackId consultado (el /status no reenvía XmlDocumentKey).
            ProviderResult result = parseDocumentResult(response.getBody(), null, response.getStatusCode().value(), trackId);
            // Conserva el trackId mientras siga PENDIENTE para reintentar en el siguiente ciclo.
            return Optional.of(result.status() == DianStatus.PENDIENTE ? pending(trackId) : result);
        } catch (RestClientResponseException | ResourceAccessException e) {
            return Optional.of(pending(trackId)); // no concluyente → reintentar, nunca un terminal especulativo
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
        String expiresRaw = firstNonNull(text(response, "expires_at"), text(at(response, "data"), "expires_at"));
        tokenCache.put(cacheKey, new CachedToken(token, tokenExpiry(expiresRaw)));
        return token;
    }

    /**
     * Expiración del token cacheado: usa el {@code expires_at} del login (formato {@code "yyyy-MM-dd HH:mm:ss"};
     * en el sandbox el token dura ~3 meses) con 1 min de margen; si no viene o no parsea, cae al TTL por defecto.
     */
    private static LocalDateTime tokenExpiry(String expiresAt) {
        if (expiresAt != null && !expiresAt.isBlank()) {
            try {
                return LocalDateTime.parse(expiresAt.trim().replace(" ", "T")).minusMinutes(1);
            } catch (RuntimeException ignored) {
                // formato inesperado → TTL por defecto
            }
        }
        return LocalDateTime.now().plusSeconds(TOKEN_TTL_SECONDS);
    }

    private record CachedToken(String token, LocalDateTime expiresAt) {
        boolean isFresh() {
            return LocalDateTime.now().isBefore(expiresAt);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Ciudades (DANE → city_id MATIAS)
    // ---------------------------------------------------------------------------------------------

    /** Traduce el código DANE del adquiriente al city_id interno de MATIAS (GET /cities, cacheado). Fallback al default. */
    private String resolveCityId(ProviderConfigSnapshot config, String daneCode) {
        if (daneCode == null || daneCode.isBlank()) return EX_CITY_ID;
        Map<String, String> cities = cityCacheByBase.computeIfAbsent(base(config), b -> loadCities(config));
        return cities.getOrDefault(daneCode, EX_CITY_ID);
    }

    /** Carga el catálogo de ciudades de MATIAS una vez por base URL: {@code dataRecords.data[].{city_code,id}}. */
    private Map<String, String> loadCities(ProviderConfigSnapshot config) {
        try {
            String token = authenticate(config);
            JsonNode response = restClient.get()
                    .uri(base(config) + "/cities")
                    .header("Authorization", "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = at(at(response, "dataRecords"), "data");
            if (data == null) data = at(response, "data");
            Map<String, String> map = new HashMap<>();
            if (data != null && data.isArray()) {
                for (JsonNode city : data) {
                    String code = text(city, "city_code");
                    String id = text(city, "id");
                    if (code != null && id != null) map.put(code, id);
                }
            }
            return map;
        } catch (RuntimeException e) {
            return Map.of(); // si el catálogo no carga, todo cae al city_id por defecto
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

    /** Endpoints donde MATIAS asigna el consecutivo (auto-increment): NO se debe enviar document_number. */
    private static boolean isAutoIncrement(ElectronicDocumentType type) {
        return type == ElectronicDocumentType.DOC_EQUIV_POS;
    }

    /** type_document_id MATIAS — confirmado contra el glosario (Códigos de Documentos, columna ID API). */
    private static int typeDocumentId(ElectronicDocumentType type) {
        return switch (type) {
            case FE_VENTA -> 7;       // Factura de Venta (DIAN 01)
            case DOC_EQUIV_POS -> 20; // Documento Equivalente POS (DIAN 20)
            case NOTA_CREDITO -> 5;   // Nota Crédito (DIAN 91)
            case NOTA_DEBITO -> 4;    // Nota Débito (DIAN 92)
        };
    }

    /**
     * identity_document_id MATIAS desde el tipo de documento del adquiriente. Valores VERIFICADOS contra el
     * catálogo en vivo {@code GET /identity-documents} (el `code` es el código DIAN). El snapshot guarda el
     * nombre del enum de Owner (CEDULA_CIUDADANIA=13, NIT=31, CEDULA_EXTRANJERIA=22, PASAPORTE=41, PEP=47).
     */
    private static String identityDocumentId(String documentType) {
        if (documentType == null) return "1";
        return switch (documentType) {
            case "CEDULA_CIUDADANIA" -> "1";   // DIAN 13
            case "CEDULA_EXTRANJERIA" -> "2";  // DIAN 22
            case "NIT" -> "3";                 // DIAN 31
            case "TARJETA_IDENTIDAD" -> "7";   // DIAN 12
            case "PASAPORTE" -> "9";           // DIAN 41
            case "PEP" -> "14";                // DIAN 47
            default -> "1";
        };
    }

    /** type_organization_id MATIAS: 1 = persona jurídica, 2 = persona natural. */
    private static int typeOrganizationId(String personType) {
        return "JURIDICA".equals(personType) ? 1 : 2;
    }

    /**
     * tax_regime_id MATIAS: 1 = Responsable de IVA, 2 = No responsable de IVA. Usa el régimen REAL congelado
     * del adquiriente ({@code Owner.taxRegime} → snapshot). Para documentos antiguos sin ese dato (null), cae
     * a la heurística: Responsable de IVA si es persona jurídica o se identifica con NIT.
     */
    private static int taxRegimeId(CustomerSnapshot customer) {
        TaxRegime regime = customer.taxRegime();
        if (regime == TaxRegime.RESPONSABLE_IVA) return TAX_REGIME_RESPONSABLE_IVA;
        if (regime == TaxRegime.NO_RESPONSABLE_IVA) return TAX_REGIME_NO_RESPONSABLE_IVA;
        // Documentos antiguos sin régimen congelado (null): heurística por tipo de persona / documento.
        boolean responsableIva = "JURIDICA".equals(customer.personType()) || "NIT".equals(customer.documentType());
        return responsableIva ? TAX_REGIME_RESPONSABLE_IVA : TAX_REGIME_NO_RESPONSABLE_IVA;
    }

    /**
     * tax_level_id MATIAS (responsabilidad fiscal / TaxLevelCode del adquiriente). Usa la responsabilidad REAL
     * congelada del adquiriente ({@code Owner.fiscalResponsibility} → snapshot). Documentos antiguos sin el dato
     * (null) o NO_APLICA → R-99-PN (5), el caso por defecto de la inmensa mayoría de adquirientes.
     */
    private static int taxLevelId(CustomerSnapshot customer) {
        FiscalResponsibility fr = customer.fiscalResponsibility();
        if (fr == null) return TAX_LEVEL_NO_APLICA;
        return switch (fr) {
            case GRAN_CONTRIBUYENTE -> TAX_LEVEL_GRAN_CONTRIBUYENTE;
            case AUTORRETENEDOR -> TAX_LEVEL_AUTORRETENEDOR;
            case AGENTE_RETENCION_IVA -> TAX_LEVEL_AGENTE_RETENCION_IVA;
            case REGIMEN_SIMPLE -> TAX_LEVEL_REGIMEN_SIMPLE;
            case NO_APLICA -> TAX_LEVEL_NO_APLICA;
        };
    }

    /**
     * operation_type_id MATIAS (verificado contra GET /operation-type): venta/POS estándar=1; las notas que
     * referencian una factura usan NC=12 / ND=14 (nuestras notas siempre referencian la factura original).
     */
    private static int operationTypeId(ElectronicDocumentType type) {
        return switch (type) {
            case FE_VENTA, DOC_EQUIV_POS -> OPERATION_TYPE_NACIONAL; // "Estandar" = 1
            case NOTA_CREDITO -> 12; // Nota Crédito que referencia una factura electrónica
            case NOTA_DEBITO -> 14;  // Nota Débito que referencia una factura electrónica
        };
    }

    /** Consumidor final = adquiriente genérico (NIT 222222222222): MATIAS espera el documento SIN customer. */
    private static boolean isFinalConsumer(CustomerSnapshot customer) {
        return customer != null && CustomerSnapshot.FINAL_CONSUMER_DOCUMENT.equals(customer.documentId());
    }

    /** tax_id MATIAS desde el esquema de la línea: IVA=1, INC=4 (verificados contra GET /taxes). */
    private static String taxIdFor(TaxScheme scheme) {
        return scheme == TaxScheme.INC ? TAX_ID_INC : TAX_ID_IVA;
    }

    private Map<String, Object> buildRequest(ElectronicDocument doc, ProviderConfigSnapshot config) {
        Map<String, Object> body = new LinkedHashMap<>();
        // Numeración fiscal asignada en la emisión desde la NumberingResolution de la empresa (F4/F1).
        // El fallback EX_* solo cubre un documento sin numerar (no debería ocurrir en el flujo de emisión).
        body.put("resolution_number", doc.getResolutionNumber() != null ? doc.getResolutionNumber() : EX_RESOLUTION_NUMBER);
        body.put("prefix", doc.getPrefix() != null ? doc.getPrefix() : EX_PREFIX);
        // En endpoints auto-increment (POS) MATIAS asigna el consecutivo y RECHAZA con 422 si se envía
        // document_number ("Para la generación automática, no debe enviar el campo document_number."). En los
        // manuales (factura / notas) es obligatorio y MATIAS respeta el que enviamos. Por eso solo va aquí.
        if (!isAutoIncrement(doc.getDocumentType())) {
            body.put("document_number", String.valueOf(doc.getConsecutive() != null ? doc.getConsecutive() : doc.getId()));
        }
        body.put("notes", "Documento " + doc.getId());
        body.put("operation_type_id", operationTypeId(doc.getDocumentType()));
        body.put("type_document_id", typeDocumentId(doc.getDocumentType()));
        body.put("graphic_representation", 0);
        body.put("send_email", 0); // la representación/correo la genera el sistema (Gotenberg), no MATIAS.
        body.put("payments", buildPayments(doc));
        // Consumidor final: los ejemplos oficiales (invoice-final-consumer / pos-final-consumer) OMITEN
        // el bloque customer. Solo lo enviamos cuando el adquiriente está identificado.
        if (!isFinalConsumer(doc.getCustomer())) {
            body.put("customer", buildCustomer(doc, config));
        }
        body.put("lines", buildLines(doc));
        body.put("legal_monetary_totals", buildMonetaryTotals(doc));
        body.put("tax_totals", buildTaxTotals(doc));

        if (doc.getDocumentType() == ElectronicDocumentType.DOC_EQUIV_POS) {
            body.put("document_signature", buildDocumentSignature());
            body.put("point_of_sale", buildPointOfSale(doc));
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
        payment.put("payment_method_id", 1); // contado: el crédito no está soportado
        // Verificado contra GET /payment-means: efectivo=10, consignación=42, tarjeta crédito=48, débito=49
        // (== nuestros códigos DIAN de PaymentMeans), así que primaryPaymentMeansCode() ya da el means_payment_id.
        payment.put("means_payment_id", parseIntOr(doc.primaryPaymentMeansCode(), MEANS_PAYMENT_EFECTIVO));
        payment.put("value_paid", str(doc.getPayableAmount()));
        return List.of(payment);
    }

    private Map<String, Object> buildCustomer(ElectronicDocument doc, ProviderConfigSnapshot config) {
        Map<String, Object> customer = new LinkedHashMap<>();
        String companyName = doc.getCustomer().legalName() != null
                ? doc.getCustomer().legalName() : doc.getCustomer().name();
        customer.put("company_name", companyName);
        customer.put("dni", doc.getCustomer().documentId());
        if (doc.getCustomer().email() != null) {
            customer.put("email", doc.getCustomer().email());
        }
        // Verificados contra catálogos en vivo: país, tipo de documento del adquiriente y tipo de organización.
        customer.put("country_id", COUNTRY_COLOMBIA);
        // city_id = id de GET /cities cuyo city_code == DANE de la ciudad del adquiriente (cache por base URL).
        customer.put("city_id", resolveCityId(config, doc.getCustomer().cityDaneCode()));
        customer.put("identity_document_id", identityDocumentId(doc.getCustomer().documentType()));
        customer.put("type_organization_id", typeOrganizationId(doc.getCustomer().personType()));
        customer.put("tax_regime_id", taxRegimeId(doc.getCustomer())); // régimen inferido del adquiriente
        customer.put("tax_level_id", taxLevelId(doc.getCustomer())); // responsabilidad fiscal real del adquiriente
        return customer;
    }

    private List<Map<String, Object>> buildLines(ElectronicDocument doc) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ElectronicDocumentLine line : doc.getLines()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("invoiced_quantity", str(line.getQuantity()));
            // Traduce el código DIAN/UNECE de la línea al id interno de MATIAS; "unidad" (94→70) por defecto.
            item.put("quantity_units_id",
                    QUANTITY_UNIT_ID_BY_DIAN_CODE.getOrDefault(line.getUnitMeasureCode(), UNIT_ID_UNIDAD));
            item.put("line_extension_amount", str(line.getLineExtensionAmount()));
            item.put("free_of_charge_indicator", false);
            item.put("description", line.getDescription());
            item.put("code", String.valueOf(line.getLineNumber()));
            item.put("type_item_identifications_id", ITEM_IDENT_CONTRIBUYENTE);
            item.put("reference_price_id", EX_REFERENCE_PRICE_ID);
            item.put("price_amount", str(line.getUnitPrice()));
            item.put("base_quantity", str(line.getQuantity()));
            item.put("tax_totals", lineTaxTotals(line));
            items.add(item);
        }
        return items;
    }

    /**
     * tax_totals de una línea. Vacío solo cuando la línea NO lleva esquema (EXCLUIDO). El EXENTO sí lleva
     * esquema IVA con tarifa 0% e importe 0: la DIAN exige reportarlo como subtotal IVA al 0% (lo que lo
     * diferencia de un excluido). GRAVADO/INC reportan su tarifa real.
     */
    private static List<Map<String, Object>> lineTaxTotals(ElectronicDocumentLine line) {
        if (line.getTaxScheme() == null) {
            return List.of();
        }
        BigDecimal percent = line.getTaxRate() == null ? BigDecimal.ZERO : line.getTaxRate();
        Map<String, Object> tax = new LinkedHashMap<>();
        tax.put("tax_id", taxIdFor(line.getTaxScheme()));
        tax.put("tax_amount", line.getTaxAmount());
        tax.put("taxable_amount", line.getLineExtensionAmount());
        tax.put("percent", percent);
        return List.of(tax);
    }

    /** tax_totals del documento: impuestos de las líneas (IVA/INC) + retenciones del adquiriente (F6). */
    private List<Map<String, Object>> buildTaxTotals(ElectronicDocument doc) {
        List<Map<String, Object>> totals = new ArrayList<>();
        for (ElectronicDocumentLine line : doc.getLines()) {
            totals.addAll(lineTaxTotals(line));
        }
        // F6 - retenciones del adquiriente (agente retenedor) como tax_totals adicionales. ReteFuente/ReteICA
        // sobre la base; ReteIVA sobre el IVA generado. Solo se incluyen las que tienen monto.
        BigDecimal base = doc.getLineExtensionAmount();
        BigDecimal iva = doc.getTaxInclusiveAmount().subtract(doc.getTaxExclusiveAmount());
        addRetention(totals, TAX_ID_RETEIVA, doc.getReteIvaAmount(), iva);
        addRetention(totals, TAX_ID_RETEFUENTE, doc.getReteFuenteAmount(), base);
        addRetention(totals, TAX_ID_RETEICA, doc.getReteIcaAmount(), base);
        return totals;
    }

    private static void addRetention(List<Map<String, Object>> totals, String taxId,
                                     BigDecimal amount, BigDecimal base) {
        if (amount == null || amount.signum() <= 0) return;
        Map<String, Object> tax = new LinkedHashMap<>();
        tax.put("tax_id", taxId);
        tax.put("tax_amount", amount);
        tax.put("taxable_amount", base);
        tax.put("percent", retentionPercent(amount, base));
        totals.add(tax);
    }

    private static BigDecimal retentionPercent(BigDecimal amount, BigDecimal base) {
        if (base == null || base.signum() == 0) return BigDecimal.ZERO;
        // Tasa efectiva de retención = amount/base·100, con la escala/redondeo monetario compartido.
        return amount.multiply(BigDecimal.valueOf(100)).divide(base, Money.SCALE, Money.ROUND);
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

    private Map<String, Object> buildPointOfSale(ElectronicDocument doc) {
        Map<String, Object> pos = new LinkedHashMap<>();
        pos.put("cashier_name", "Cajero");
        pos.put("terminal_number", "CJ001");
        pos.put("cashier_type", "Caja principal");
        pos.put("sales_code", "POS01");
        pos.put("address", "N/A");
        pos.put("sub_total", str(doc.getTaxExclusiveAmount())); // requerido por MATIAS: subtotal sin impuestos
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

    /**
     * Interpreta la respuesta de emisión/estado de MATIAS (docs: response-json). Campos reales:
     * {@code response.StatusCode} ("00"=autorizado, "98"=en proceso, "02"=duplicado), {@code success},
     * {@code XmlDocumentKey}=CUFE/CUDE, objetos {@code qr/pdf/AttachedDocument}. HTTP 201 / send_to_queue=1
     * = encolado (async). {@code doc} puede ser null (polling sin contexto de tipo): el sello va en cufe.
     */
    private ProviderResult parseDocumentResult(JsonNode body, ElectronicDocument doc, int http, String fallbackKey) {
        JsonNode response = at(body, "response");
        String statusCode = text(response, "StatusCode");
        // El sello viene en XmlDocumentKey al emitir; el POST /status (reconciliación) NO lo reenvía, así que
        // ahí el sello es el propio trackId consultado (fallbackKey = CUFE/CUDE que ya teníamos).
        String key = firstNonNull(text(body, "XmlDocumentKey"), text(response, "XmlDocumentKey"), fallbackKey);
        boolean queued = http == 201 || intOf(body, "send_to_queue") == 1 || "98".equals(statusCode);

        if ("00".equals(statusCode)) {
            // El sello fiscal (CUFE/CUDE/CUDS) es un SHA-384 = 96 hex. Si MATIAS reporta 00 pero el sello no
            // tiene formato válido, NO marcamos VALIDADO sin sello: alertamos y dejamos PENDIENTE para que la
            // reconciliación (fetchStatus) traiga el sello real (evita un documento "validado" sin CUFE/CUDE).
            if (!isValidSeal(key)) {
                log.warn("MATIAS reportó StatusCode 00 sin sello CUFE/CUDE válido (esperado SHA-384, 96 hex): '{}'."
                        + " Documento {} se deja PENDIENTE para reconciliar. Respuesta: {}",
                        key, doc == null ? "(polling)" : doc.getId(), safe(body));
                return pending(firstNonNull(key, text(response, "XmlFileName"), extractTrackId(body)));
            }
            JsonNode qr = at(body, "qr");
            JsonNode attached = at(body, "AttachedDocument");
            boolean isInvoice = doc == null || doc.getDocumentType() == ElectronicDocumentType.FE_VENTA;
            String cufe = isInvoice ? key : null;   // FE → CUFE
            String cude = isInvoice ? null : key;   // POS/notas → CUDE
            // En POS (auto-increment) el consecutivo lo asigna MATIAS; lo extraemos del StatusMessage
            // ("...electrónica DPOS1, ha sido autorizada.") para sellar el número fiscal REAL en el documento
            // (en facturas/notas coincide con el que enviamos, así que es inocuo). markValidated solo lo aplica
            // si viene poblado.
            String[] assigned = parseAssignedNumber(text(response, "StatusMessage"));
            return new ProviderResult(DianStatus.VALIDADO, assigned[0], parseLongOrNull(assigned[1]), cufe, cude, null,
                    text(attached, "url"),                                // xmlSigned: URL del XML firmado
                    firstNonNull(text(qr, "qrDian"), text(qr, "data")),   // qrData: contenido/URL DIAN
                    text(qr, "url"),                                      // qrUrl: imagen QR
                    null,                                                 // pdf: lo genera nuestra entrega (Gotenberg)
                    key, null, LocalDateTime.now(), http, safe(body));
        }
        if (queued) {
            String trackId = firstNonNull(key, text(response, "XmlFileName"), extractTrackId(body));
            return pending(trackId);
        }
        // Negativo concluyente (success=false o StatusCode distinto de 00/98) → rechazo; sin señal → pendiente.
        boolean negative = (body != null && !body.path("success").asBoolean(true)) || statusCode != null;
        if (!negative) return pending(key);
        String reason = firstNonNull(text(response, "StatusMessage"), text(body, "message"),
                text(response, "StatusDescription"));
        return new ProviderResult(DianStatus.RECHAZADO, null, null, null, null, null, null, null, null, null,
                key, reason, null, http, safe(body));
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

    /**
     * Extrae [prefix, consecutivo] del {@code StatusMessage} de MATIAS (p. ej.
     * "La Factura electrónica DPOS1, ha sido autorizada.") — el patrón "LETRAS+DÍGITOS," del número fiscal.
     * Útil para POS, donde el consecutivo lo asigna MATIAS. Devuelve {@code [null, null]} si no se reconoce.
     */
    private static final java.util.regex.Pattern ASSIGNED_NUMBER =
            java.util.regex.Pattern.compile("([A-Za-z]{1,10})(\\d{1,18}),");

    private static String[] parseAssignedNumber(String statusMessage) {
        if (statusMessage != null) {
            java.util.regex.Matcher m = ASSIGNED_NUMBER.matcher(statusMessage);
            if (m.find()) return new String[]{m.group(1), m.group(2)};
        }
        return new String[]{null, null};
    }

    private static Long parseLongOrNull(String value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** El sello fiscal (CUFE/CUDE/CUDS) debe ser un SHA-384: exactamente 96 caracteres hexadecimales. */
    private static boolean isValidSeal(String key) {
        return key != null && SEAL_SHA384.matcher(key).matches();
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

    private static int intOf(JsonNode node, String key) {
        if (node == null) return 0;
        JsonNode value = node.get(key);
        return value == null || value.isNull() ? 0 : value.asInt(0);
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
