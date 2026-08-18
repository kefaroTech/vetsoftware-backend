package com.vetsoftware.app.electronicdocument.infrastructure.provider.matias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.BranchInfoQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.EmployeeNameQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderResult;
import com.vetsoftware.app.electronicdocument.application.port.out.TechnicalKeyQueryPort;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.DocumentReference;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.FiscalResponsibility;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.TaxRegime;
import com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * MatiasInvoiceProvider — adaptador HTTP contra el proveedor DIAN MATIAS.
 *
 * <p>
 * El {@link RestClient} se mockea eslabon por eslabon (sin {@code
 * RETURNS_DEEP_STUBS}: falla en silencio en {@code header(String, String...)}
 * por su firma varargs + auto-tipo genérico, devolviendo {@code null} en vez de
 * encadenar). Cada interfaz de la cadena fluida tiene su propio mock, y
 * {@link #montarCadenaRestClient()} cablea el paso de uno a otro una sola vez;
 * los tres caminos reales (login sin {@code header()}, envio con
 * {@code header()}, GET de ciudades) conviven porque comparten los mismos mocks
 * intermedios y solo divergen en el {@code retrieve()} final.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatiasInvoiceProvider — adaptador HTTP MATIAS (UBL 2.1)")
class MatiasInvoiceProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SELLO_VALIDO = "a1b2c3d4e5f6".repeat(8); // 96 hex

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec postUriSpec;
    // RETURNS_SELF: header()/contentType()/accept()/body() son metodos autotipados
    // (devuelven "this" en la API real de RestClient) — es la respuesta hecha
    // para builders fluidos, y evita adivinar la firma exacta de cada eslabon.
    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestBodySpec postBodySpec;
    @Mock
    private RestClient.ResponseSpec postResponseSpec;
    @Mock
    private RestClient.RequestHeadersUriSpec getUriSpec;
    @Mock(answer = Answers.RETURNS_SELF)
    private RestClient.RequestHeadersSpec getSendSpec;
    @Mock
    private RestClient.ResponseSpec getResponseSpec;
    @Mock
    private TechnicalKeyQueryPort technicalKeyQueryPort;
    @Mock
    private EmployeeNameQueryPort employeeNameQueryPort;
    @Mock
    private BranchInfoQueryPort branchInfoQueryPort;

    private final MatiasPosConfig posConfig = new MatiasPosConfig("CJ001", "Caja principal",
            "POS01", "Config N/A", "Cajero por defecto", "VetSoftware SAS", "VetSoftware",
            "VetSoftware");

    private MatiasInvoiceProvider provider;

    @BeforeEach
    void montar() {
        provider = new MatiasInvoiceProvider(restClient, technicalKeyQueryPort,
                employeeNameQueryPort, branchInfoQueryPort, posConfig);
    }

    /**
     * Cablea los eslabones de la cadena fluida una sola vez. {@code lenient()}
     * porque ningun test individual recorre los dos caminos (POST y GET) ni las dos
     * ramas de POST ({@code body()} para envio/login vs sin {@code body()} para
     * fetchStatus) a la vez: cada uno usa un subconjunto.
     */
    private void montarCadenaRestClient() {
        lenient().when(restClient.post()).thenReturn(postUriSpec); // cableado
        lenient().when(postUriSpec.uri(anyString())).thenReturn(postBodySpec); // cableado
        lenient().when(postBodySpec.retrieve()).thenReturn(postResponseSpec); // cableado

        lenient().when(restClient.get()).thenReturn(getUriSpec); // cableado
        lenient().when(getUriSpec.uri(anyString())).thenReturn(getSendSpec); // cableado
        lenient().when(getSendSpec.retrieve()).thenReturn(getResponseSpec); // cableado
    }

    /**
     * buildRequest siempre consulta la clave tecnica activa; se stubea solo en los
     * tests que llegan a transmit() (fetchStatus no la toca, y STRICT_STUBS
     * marcaria un stub sin usar como error).
     */
    private void stubSinClaveTecnica() {
        // lenient: en los tests donde authenticate() lanza antes de llegar a
        // buildRequest() (p. ej. login sin access_token) este stub no llega a usarse.
        lenient().when(technicalKeyQueryPort.findActiveTechnicalKey(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    private static ProviderConfigSnapshot configConLogin() {
        return new ProviderConfigSnapshot("MATIAS", "https://sandbox.matias-api.com/api/ubl2.1",
                null, null, "user@vet.co", "secreto", null, "whsecret", null);
    }

    private static ProviderConfigSnapshot configConPat() {
        return new ProviderConfigSnapshot("MATIAS", "https://sandbox.matias-api.com/api/ubl2.1",
                null, null, null, null, "PAT-FIJO-123", "whsecret", null);
    }

    // --- Respuestas del proveedor: todas terminan en
    // postResponseSpec/getResponseSpec,
    // los mismos mocks para las tres formas de la cadena (login, envio,
    // fetchStatus). ---

    private void stubLogin(String json) {
        when(postResponseSpec.body(JsonNode.class)).thenReturn(MAPPER.readTree(json));
    }

    private void stubTransmitResponse(int httpStatus, String json) {
        JsonNode body = json == null ? null : MAPPER.readTree(json);
        when(postResponseSpec.toEntity(JsonNode.class))
                .thenReturn(ResponseEntity.status(httpStatus).body(body));
    }

    private void stubTransmitThrows(RuntimeException ex) {
        when(postResponseSpec.toEntity(JsonNode.class)).thenThrow(ex);
    }

    private void stubFetchStatusResponse(int httpStatus, String json) {
        stubTransmitResponse(httpStatus, json);
    }

    private void stubFetchStatusThrows(RuntimeException ex) {
        stubTransmitThrows(ex);
    }

    private void stubCities(String json) {
        when(getResponseSpec.body(JsonNode.class)).thenReturn(MAPPER.readTree(json));
    }

    private void stubCitiesThrows() {
        when(getResponseSpec.body(JsonNode.class))
                .thenThrow(new ResourceAccessException("catalogo caido"));
    }

    /**
     * postBodySpec.body(...) recibe tanto las credenciales de login (si hubo que
     * autenticar, incluida la re-autenticacion para el catalogo de ciudades) como
     * el cuerpo final de la factura/POS/nota — el ultimo valor capturado es siempre
     * este ultimo, porque buildRequest() (con sus posibles logins anidados) termina
     * antes de que transmit() haga su propio body(...).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> capturarCuerpoEnviado() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(postBodySpec, org.mockito.Mockito.atLeastOnce()).body(captor.capture());
        List<Object> valores = captor.getAllValues();
        return (Map<String, Object>) valores.get(valores.size() - 1);
    }

    // --- Fixtures propias (no se toca ElectronicDocumentMother, solo se usa) ---

    private static ElectronicDocument documento(ElectronicDocumentType type,
            CustomerSnapshot customer, String prefix, Long consecutive, String resolutionNumber,
            Long branchId, List<ElectronicDocumentLine> lines, BigDecimal reteFuente,
            BigDecimal reteIva, BigDecimal reteIca, DocumentReference reference,
            String noteReasonCode) {
        BigDecimal base = lines.stream().map(ElectronicDocumentLine::getLineExtensionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = lines.stream().map(ElectronicDocumentLine::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ElectronicDocumentPayment> payments = List.of(new ElectronicDocumentPayment(null,
                com.vetsoftware.app.electronicdocument.domain.PaymentMeans.EFECTIVO, total));
        return new ElectronicDocument(9L, ElectronicDocumentMother.COMPANY_ID, 100L, type, prefix,
                consecutive, resolutionNumber, LocalDate.of(2026, 3, 10), "10:15:00-05:00", null,
                null, null, null, null, null, null, DianStatus.PENDIENTE, null,
                ElectronicDocumentMother.issuer(), customer, base, base, total, total,
                PaymentForm.CONTADO, lines, payments, LocalDateTime.of(2026, 3, 10, 10, 15), true,
                reference, noteReasonCode, noteReasonCode == null ? null : "Anulacion", false,
                reteFuente, reteIva, reteIca, null, ElectronicDocumentMother.EMPLOYEE_ID, branchId);
    }

    private static ElectronicDocument facturaBasica() {
        return documento(ElectronicDocumentType.FE_VENTA, ElectronicDocumentMother.customer(), null,
                null, null, ElectronicDocumentMother.BRANCH_ID,
                ElectronicDocumentMother.unaLineaGravada(), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, null);
    }

    private static CustomerSnapshot clienteSinCiudad() {
        return new CustomerSnapshot("CEDULA_CIUDADANIA", "1020304050", "3", "NATURAL", null,
                "Ana Perez", "ana@correo.co", null, TaxRegime.NO_RESPONSABLE_IVA,
                FiscalResponsibility.NO_APLICA);
    }

    /**
     * Sin cityDaneCode: resolveCityId no dispara loadCities() (que a su vez
     * volveria a llamar authenticate()). La usan los tests de autenticacion para
     * que el conteo de logins mida solo lo que dicen medir.
     */
    private static ElectronicDocument facturaSinCiudad() {
        return documento(ElectronicDocumentType.FE_VENTA, clienteSinCiudad(), null, null, null,
                ElectronicDocumentMother.BRANCH_ID, ElectronicDocumentMother.unaLineaGravada(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }

    // ============================================================
    @Nested
    @DisplayName("providerName")
    class NombreProveedor {
        @Test
        @DisplayName("es MATIAS")
        void es_matias() {
            assertThat(provider.providerName()).isEqualTo("MATIAS");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("transmit — autenticacion")
    class Autenticacion {

        @BeforeEach
        void sinClaveTecnica() {
            montarCadenaRestClient();
            stubSinClaveTecnica();
        }

        @Test
        @DisplayName("con credenciales hace login y cachea el token con el expires_at del proveedor")
        void login_exitoso_cachea_token() {
            stubLogin("""
                    {"access_token": "tok-123", "expires_at": "2099-01-01 00:00:00"}
                    """);
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            ProviderResult result = provider.transmit(facturaSinCiudad(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.VALIDADO);
        }

        @Test
        @DisplayName("sin expires_at cae al TTL por defecto sin lanzar")
        void login_sin_expires_at_usa_ttl_default() {
            stubLogin("""
                    {"access_token": "tok-456"}
                    """);
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            ProviderResult result = provider.transmit(facturaSinCiudad(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.VALIDADO);
        }

        @Test
        @DisplayName("el access_token anidado en data tambien se reconoce")
        void login_token_anidado_en_data() {
            stubLogin("""
                    {"data": {"access_token": "tok-nested", "expires_at": "2099-01-01 00:00:00"}}
                    """);
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            ProviderResult result = provider.transmit(facturaSinCiudad(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.VALIDADO);
        }

        @Test
        @DisplayName("sin access_token en la respuesta lanza IllegalStateException")
        void login_sin_token_lanza() {
            stubLogin("{}");

            org.assertj.core.api.Assertions
                    .assertThatThrownBy(
                            () -> provider.transmit(facturaSinCiudad(), configConLogin()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no devolvió access_token");
        }

        @Test
        @DisplayName("con apiToken (PAT) no hace login y lo usa como Bearer")
        void pat_evita_login() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            ProviderResult result = provider.transmit(facturaSinCiudad(), configConPat());

            assertThat(result.status()).isEqualTo(DianStatus.VALIDADO);
            verify(postUriSpec, never()).uri(contains("/auth/login"));
            ArgumentCaptor<String> valorHeader = ArgumentCaptor.forClass(String.class);
            verify(postBodySpec).header(eq("Authorization"), valorHeader.capture());
            assertThat(valorHeader.getValue()).isEqualTo("Bearer PAT-FIJO-123");
        }

        @Test
        @DisplayName("un token cacheado y fresco se reutiliza: el login solo ocurre una vez")
        void token_fresco_se_reutiliza() {
            stubLogin("""
                    {"access_token": "tok-cache", "expires_at": "2099-01-01 00:00:00"}
                    """);
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            provider.transmit(facturaSinCiudad(), configConLogin());
            provider.transmit(facturaSinCiudad(), configConLogin());

            verify(postUriSpec, times(1)).uri(contains("/auth/login"));
        }

        @Test
        @DisplayName("un token cacheado pero expirado fuerza un nuevo login")
        void token_expirado_fuerza_renovacion() {
            stubLogin("""
                    {"access_token": "tok-viejo", "expires_at": "2000-01-01 00:00:00"}
                    """);
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            provider.transmit(facturaSinCiudad(), configConLogin());
            provider.transmit(facturaSinCiudad(), configConLogin());

            verify(postUriSpec, times(2)).uri(contains("/auth/login"));
        }
    }

    // ============================================================
    @Nested
    @DisplayName("transmit — construccion del payload")
    class ConstruccionPayload {

        @BeforeEach
        void loginListo() {
            montarCadenaRestClient();
            stubSinClaveTecnica();
            stubLogin("""
                    {"access_token": "tok", "expires_at": "2099-01-01 00:00:00"}
                    """);
        }

        @Test
        @DisplayName("con numeracion propia, envia resolution_number/prefix/document_number reales")
        void numeracion_propia_va_en_el_cuerpo() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP991, ok."));

            provider.transmit(documento(ElectronicDocumentType.FE_VENTA,
                    ElectronicDocumentMother.customer(), "SETP", 991L, "18760000099",
                    ElectronicDocumentMother.BRANCH_ID, ElectronicDocumentMother.unaLineaGravada(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                    configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            assertThat(body.get("resolution_number")).isEqualTo("18760000099");
            assertThat(body.get("prefix")).isEqualTo("SETP");
            assertThat(body.get("document_number")).isEqualTo("991");
        }

        @Test
        @DisplayName("sin numeracion cae a los defaults EX_RESOLUTION_NUMBER/EX_PREFIX")
        void sin_numeracion_cae_al_default() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            provider.transmit(facturaBasica(), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            assertThat(body.get("resolution_number")).isEqualTo("18764074347312");
            assertThat(body.get("prefix")).isEqualTo("SETP");
        }

        @Test
        @DisplayName("en DOC_EQUIV_POS (auto-increment) NO se envia document_number")
        void auto_increment_no_envia_document_number() {
            stubTransmitResponse(201, "{\"response\":{\"StatusCode\":\"98\"}}");

            provider.transmit(ElectronicDocumentMother.posPendiente(), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            assertThat(body).doesNotContainKey("document_number");
        }

        @Test
        @DisplayName("consumidor final: la clave customer no viaja en el cuerpo")
        void consumidor_final_omite_customer() {
            stubTransmitResponse(201, "{\"response\":{\"StatusCode\":\"98\"}}");

            provider.transmit(ElectronicDocumentMother.posPendiente(), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            assertThat(body).doesNotContainKey("customer");
        }

        @Test
        @DisplayName("cliente identificado sin ciudad cae al city_id por defecto sin llamar al catalogo")
        void cliente_sin_ciudad_cae_a_default_sin_llamar_catalogo() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            provider.transmit(documento(ElectronicDocumentType.FE_VENTA, clienteSinCiudad(), null,
                    null, null, ElectronicDocumentMother.BRANCH_ID,
                    ElectronicDocumentMother.unaLineaGravada(), BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, null, null), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) body.get("customer");
            assertThat(customer.get("city_id")).isEqualTo("959");
            verify(restClient, never()).get();
        }

        @Test
        @DisplayName("cliente con ciudad presente en el catalogo usa el city_id mapeado")
        void cliente_con_ciudad_en_catalogo() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
            stubCities("""
                    {"dataRecords": {"data": [{"city_code": "05001", "id": "777"}]}}
                    """);

            provider.transmit(facturaBasica(), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) body.get("customer");
            assertThat(customer.get("city_id")).isEqualTo("777");
        }

        @Test
        @DisplayName("cliente con ciudad ausente del catalogo cae al city_id por defecto")
        void cliente_con_ciudad_fuera_del_catalogo() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
            stubCities("""
                    {"dataRecords": {"data": [{"city_code": "11001", "id": "1"}]}}
                    """);

            provider.transmit(facturaBasica(), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) body.get("customer");
            assertThat(customer.get("city_id")).isEqualTo("959");
        }

        @Test
        @DisplayName("si el catalogo de ciudades falla al cargar, cae al city_id por defecto sin lanzar")
        void catalogo_de_ciudades_falla_cae_a_default() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
            stubCitiesThrows();

            provider.transmit(facturaBasica(), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) body.get("customer");
            assertThat(customer.get("city_id")).isEqualTo("959");
        }

        @Test
        @DisplayName("el catalogo de ciudades se carga una sola vez por base URL (cache)")
        void catalogo_de_ciudades_se_cachea() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
            stubCities("""
                    {"dataRecords": {"data": [{"city_code": "05001", "id": "777"}]}}
                    """);

            provider.transmit(facturaBasica(), configConLogin());
            provider.transmit(facturaBasica(), configConLogin());

            verify(getUriSpec, times(1)).uri(contains("/cities"));
        }

        @Test
        @DisplayName("las retenciones con monto positivo se agregan a tax_totals; las de monto cero se omiten")
        void retenciones_con_monto_positivo_se_agregan() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            provider.transmit(documento(ElectronicDocumentType.FE_VENTA,
                    ElectronicDocumentMother.customer(), null, null, null,
                    ElectronicDocumentMother.BRANCH_ID, ElectronicDocumentMother.unaLineaGravada(),
                    new BigDecimal("10.00"), new BigDecimal("5.00"), BigDecimal.ZERO, null, null),
                    configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taxTotals = (List<Map<String, Object>>) body
                    .get("tax_totals");
            // 1 linea gravada (IVA) + reteFuente + reteIVA = 3; reteICA es cero y se omite.
            assertThat(taxTotals).hasSize(3);
            assertThat(taxTotals).extracting(t -> t.get("tax_id")).contains("6", "5");
        }

        @Test
        @DisplayName("el porcentaje de retencion es cero cuando la base es cero, sin dividir por cero")
        void porcentaje_de_retencion_es_cero_con_base_cero() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
            // Linea EXCLUIDA (sin esquema tributario): la base de retencion es cero, pero
            // se fuerza un monto de reteFuente > 0 para forzar la rama defensiva.
            List<ElectronicDocumentLine> lineasExcluidas = List
                    .of(ElectronicDocumentMother.excluidoLine(1, "1000.00"));

            provider.transmit(
                    documento(ElectronicDocumentType.FE_VENTA, ElectronicDocumentMother.customer(),
                            null, null, null, ElectronicDocumentMother.BRANCH_ID, lineasExcluidas,
                            new BigDecimal("10.00"), BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                    configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taxTotals = (List<Map<String, Object>>) body
                    .get("tax_totals");
            assertThat(taxTotals).anySatisfy(t -> {
                assertThat(t.get("tax_id")).isEqualTo("6");
                assertThat(t.get("percent")).isEqualTo(BigDecimal.ZERO);
            });
        }

        @Test
        @DisplayName("DOC_EQUIV_POS con sede emisora usa direccion/codigo de la sede")
        void pos_con_sede_usa_datos_de_la_sede() {
            stubTransmitResponse(201, "{\"response\":{\"StatusCode\":\"98\"}}");
            when(branchInfoQueryPort.findById(ElectronicDocumentMother.BRANCH_ID))
                    .thenReturn(Optional.of(new BranchInfoQueryPort.BranchInfo("Sede Norte", "N01",
                            "Cra 1 # 2-3")));
            when(employeeNameQueryPort.findName(ElectronicDocumentMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of("Maria Cajera"));

            provider.transmit(ElectronicDocumentMother.posPendiente(), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> pos = (Map<String, Object>) body.get("point_of_sale");
            assertThat(pos.get("address")).isEqualTo("Cra 1 # 2-3");
            assertThat(pos.get("sales_code")).isEqualTo("N01");
            assertThat(pos.get("cashier_name")).isEqualTo("Maria Cajera");
        }

        @Test
        @DisplayName("DOC_EQUIV_POS sin datos de sede cae a la config del despliegue")
        void pos_sin_sede_cae_al_config() {
            stubTransmitResponse(201, "{\"response\":{\"StatusCode\":\"98\"}}");
            when(branchInfoQueryPort.findById(ElectronicDocumentMother.BRANCH_ID))
                    .thenReturn(Optional.empty());
            when(employeeNameQueryPort.findName(ElectronicDocumentMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            provider.transmit(ElectronicDocumentMother.posPendiente(), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> pos = (Map<String, Object>) body.get("point_of_sale");
            assertThat(pos.get("address")).isEqualTo("Config N/A");
            assertThat(pos.get("sales_code")).isEqualTo("POS01");
            assertThat(pos.get("cashier_name")).isEqualTo("Cajero por defecto");
            @SuppressWarnings("unchecked")
            Map<String, Object> sw = (Map<String, Object>) body.get("software_manufacturer");
            assertThat(sw.get("software_name")).isEqualTo("VetSoftware");
        }

        @Test
        @DisplayName("una nota credito envia billing_reference y discrepancy_response con el codigo del documento")
        void nota_credito_envia_referencia_y_discrepancia() {
            stubTransmitResponse(200,
                    "{\"response\":{\"StatusCode\":\"00\"}, \"XmlDocumentKey\": \"" + SELLO_VALIDO
                            + "\"}");
            DocumentReference ref = new DocumentReference(ElectronicDocumentMother.CUFE, "SETP",
                    990L, LocalDate.of(2026, 3, 10));

            provider.transmit(documento(ElectronicDocumentType.NOTA_CREDITO,
                    ElectronicDocumentMother.customer(), null, null, null,
                    ElectronicDocumentMother.BRANCH_ID, ElectronicDocumentMother.unaLineaGravada(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, ref, "3"), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> billingRef = (Map<String, Object>) body.get("billing_reference");
            assertThat(billingRef.get("number")).isEqualTo(990L);
            assertThat(billingRef.get("uuid")).isEqualTo(ElectronicDocumentMother.CUFE);
            @SuppressWarnings("unchecked")
            Map<String, Object> discrepancy = (Map<String, Object>) body
                    .get("discrepancy_response");
            assertThat(discrepancy.get("response_id")).isEqualTo("3");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("transmit — interpretacion de la respuesta del proveedor")
    class InterpretacionRespuesta {

        @BeforeEach
        void loginListo() {
            montarCadenaRestClient();
            stubSinClaveTecnica();
            stubLogin("""
                    {"access_token": "tok", "expires_at": "2099-01-01 00:00:00"}
                    """);
        }

        @Test
        @DisplayName("StatusCode 00 con sello valido en una factura -> VALIDADO con CUFE")
        void statuscode_00_factura_valida() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.VALIDADO);
            assertThat(result.cufe()).isEqualTo(SELLO_VALIDO);
            assertThat(result.cude()).isNull();
        }

        @Test
        @DisplayName("StatusCode 00 con sello valido en un POS -> VALIDADO con CUDE y numero asignado por MATIAS")
        void statuscode_00_pos_valido_asigna_numero() {
            stubTransmitResponse(201, respuestaValidada(SELLO_VALIDO,
                    "La Factura electronica DPOS1, ha sido autorizada."));

            ProviderResult result = provider.transmit(ElectronicDocumentMother.posPendiente(),
                    configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.VALIDADO);
            assertThat(result.cude()).isEqualTo(SELLO_VALIDO);
            assertThat(result.cufe()).isNull();
            assertThat(result.prefix()).isEqualTo("DPOS");
            assertThat(result.consecutive()).isEqualTo(1L);
        }

        @Test
        @DisplayName("StatusCode 00 con sello invalido (no SHA-384) se deja PENDIENTE para reconciliar")
        void statuscode_00_sello_invalido_queda_pendiente() {
            stubTransmitResponse(200,
                    """
                            {"response": {"StatusCode": "00", "StatusMessage": "ok"}, "XmlDocumentKey": "corto"}
                            """);

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.PENDIENTE);
            assertThat(result.providerDocumentKey()).isEqualTo("corto");
        }

        @Test
        @DisplayName("HTTP 201 sin StatusCode 00 se interpreta como encolado (PENDIENTE)")
        void http_201_encolado() {
            stubTransmitResponse(201, """
                    {"response": {"StatusCode": "98"}, "XmlDocumentKey": "TRACK-1"}
                    """);

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.PENDIENTE);
            assertThat(result.providerDocumentKey()).isEqualTo("TRACK-1");
        }

        @Test
        @DisplayName("send_to_queue=1 con HTTP 200 tambien se interpreta como encolado")
        void send_to_queue_encolado() {
            stubTransmitResponse(200, """
                    {"send_to_queue": 1, "response": {}, "XmlDocumentKey": "TRACK-2"}
                    """);

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.PENDIENTE);
            assertThat(result.providerDocumentKey()).isEqualTo("TRACK-2");
        }

        @Test
        @DisplayName("un StatusCode negativo concluyente se interpreta como RECHAZADO con el motivo del proveedor")
        void statuscode_negativo_rechazado() {
            stubTransmitResponse(200,
                    """
                            {"response": {"StatusCode": "05", "StatusMessage": "Documento duplicado"}, "XmlDocumentKey": "K"}
                            """);

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.RECHAZADO);
            assertThat(result.rejectionReason()).isEqualTo("Documento duplicado");
        }

        @Test
        @DisplayName("una respuesta sin StatusCode y success=true (ambigua) se deja PENDIENTE, nunca un terminal especulativo")
        void respuesta_ambigua_queda_pendiente() {
            stubTransmitResponse(200, """
                    {"response": {}, "success": true}
                    """);

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.PENDIENTE);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("transmit — fallos del transporte")
    class Fallos {

        @BeforeEach
        void loginListo() {
            montarCadenaRestClient();
            stubSinClaveTecnica();
            stubLogin("""
                    {"access_token": "tok", "expires_at": "2099-01-01 00:00:00"}
                    """);
        }

        @Test
        @DisplayName("un 4xx del proveedor se interpreta como RECHAZADO con el cuerpo de error")
        void error_4xx_rechazado() {
            stubTransmitThrows(HttpClientErrorException.create(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unprocessable Entity", HttpHeaders.EMPTY,
                    "{\"error\":\"invalido\"}".getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8));

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.RECHAZADO);
            assertThat(result.httpStatus()).isEqualTo(422);
            assertThat(result.rejectionReason()).contains("invalido");
        }

        @Test
        @DisplayName("un 5xx del proveedor (DIAN caida) se interpreta como CONTINGENCIA para reintentar")
        void error_5xx_contingencia() {
            stubTransmitThrows(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE,
                    "Service Unavailable", HttpHeaders.EMPTY,
                    "caida".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.CONTINGENCIA);
            assertThat(result.httpStatus()).isEqualTo(503);
        }

        @Test
        @DisplayName("un timeout de conexion se interpreta como CONTINGENCIA")
        void timeout_contingencia() {
            stubTransmitThrows(new ResourceAccessException("Connection timed out"));

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.CONTINGENCIA);
            assertThat(result.rawResponse()).contains("Connection timed out");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("fetchStatus — reconciliacion por polling")
    class FetchStatus {

        @Test
        @DisplayName("un trackId null devuelve vacio sin llamar al proveedor")
        void trackid_null_no_llama() {
            Optional<ProviderResult> result = provider.fetchStatus(facturaBasica(), null,
                    configConLogin());

            assertThat(result).isEmpty();
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("un trackId en blanco devuelve vacio sin llamar al proveedor")
        void trackid_en_blanco_no_llama() {
            Optional<ProviderResult> result = provider.fetchStatus(facturaBasica(), "  ",
                    configConLogin());

            assertThat(result).isEmpty();
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("sin el documento no se consulta: no hay tipo con que decidir el sello")
        void sin_documento_no_llama() {
            Optional<ProviderResult> result = provider.fetchStatus(null, "TRACK-9",
                    configConLogin());

            assertThat(result).isEmpty();
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("reconciliando una factura de venta el sello terminal (00) se guarda como CUFE")
        void terminal_de_factura_sella_cufe() {
            montarCadenaRestClient();
            stubFetchStatusResponse(200, respuestaValidada(SELLO_VALIDO, "ok"));

            Optional<ProviderResult> result = provider.fetchStatus(facturaBasica(), "TRACK-9",
                    configConPat());

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(DianStatus.VALIDADO);
            assertThat(result.get().cufe()).isEqualTo(SELLO_VALIDO);
            assertThat(result.get().cude()).isNull();
        }

        /**
         * El sello de un POS es CUDE, nunca CUFE (Res. 000165/2023). Mientras
         * fetchStatus no recibio el documento asumia factura de venta y guardaba el
         * CUDE de todo POS reconciliado por polling en el campo cufe: un dato fiscal en
         * la columna equivocada.
         */
        @Test
        @DisplayName("reconciliando un documento equivalente POS el sello se guarda como CUDE, no como CUFE")
        void terminal_de_pos_sella_cude() {
            montarCadenaRestClient();
            stubFetchStatusResponse(200, respuestaValidada(SELLO_VALIDO, "ok"));

            Optional<ProviderResult> result = provider.fetchStatus(
                    ElectronicDocumentMother.posPendiente(), "TRACK-9", configConPat());

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(DianStatus.VALIDADO);
            assertThat(result.get().cude()).isEqualTo(SELLO_VALIDO);
            assertThat(result.get().cufe()).isNull();
        }

        @Test
        @DisplayName("un estado no concluyente (en proceso) conserva el trackId consultado, no el del cuerpo")
        void estado_pendiente_conserva_trackid_consultado() {
            montarCadenaRestClient();
            stubFetchStatusResponse(200, """
                    {"response": {"StatusCode": "98"}, "XmlDocumentKey": "OTRO-KEY"}
                    """);

            Optional<ProviderResult> result = provider.fetchStatus(facturaBasica(), "TRACK-9",
                    configConPat());

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(DianStatus.PENDIENTE);
            assertThat(result.get().providerDocumentKey()).isEqualTo("TRACK-9");
        }

        @Test
        @DisplayName("un error del proveedor (4xx/5xx) nunca lanza: devuelve PENDIENTE con el trackId")
        void error_del_proveedor_no_lanza() {
            montarCadenaRestClient();
            stubFetchStatusThrows(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found",
                    HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

            Optional<ProviderResult> result = provider.fetchStatus(facturaBasica(), "TRACK-9",
                    configConPat());

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(DianStatus.PENDIENTE);
            assertThat(result.get().providerDocumentKey()).isEqualTo("TRACK-9");
        }

        @Test
        @DisplayName("un timeout tampoco lanza: devuelve PENDIENTE con el trackId")
        void timeout_no_lanza() {
            montarCadenaRestClient();
            stubFetchStatusThrows(new ResourceAccessException("timeout"));

            Optional<ProviderResult> result = provider.fetchStatus(facturaBasica(), "TRACK-9",
                    configConPat());

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(DianStatus.PENDIENTE);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("transmit — catalogos del adquiriente y de las lineas")
    class CatalogosAdquirienteYLineas {

        @BeforeEach
        void loginListo() {
            montarCadenaRestClient();
            stubSinClaveTecnica();
            stubLogin("""
                    {"access_token": "tok", "expires_at": "2099-01-01 00:00:00"}
                    """);
        }

        private CustomerSnapshot clienteConDocumento(String documentType) {
            return new CustomerSnapshot(documentType, "1020304050", "3", "NATURAL", null,
                    "Ana Perez", "ana@correo.co", "05001", TaxRegime.NO_RESPONSABLE_IVA,
                    FiscalResponsibility.NO_APLICA);
        }

        @ParameterizedTest
        @CsvSource({"CEDULA_CIUDADANIA, 1", "CEDULA_EXTRANJERIA, 2", "NIT, 3",
                "TARJETA_IDENTIDAD, 7", "PASAPORTE, 9", "PEP, 14", "TIPO_DESCONOCIDO, 1"})
        @DisplayName("identity_document_id se traduce desde el tipo de documento del adquiriente")
        void identity_document_id_por_tipo_de_documento(String tipoDocumento, String esperado) {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            provider.transmit(documento(ElectronicDocumentType.FE_VENTA,
                    clienteConDocumento(tipoDocumento), null, null, null,
                    ElectronicDocumentMother.BRANCH_ID, ElectronicDocumentMother.unaLineaGravada(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                    configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) body.get("customer");
            assertThat(customer.get("identity_document_id")).isEqualTo(esperado);
        }

        @Test
        @DisplayName("sin tipo de documento del adquiriente, identity_document_id cae al default")
        void sin_tipo_de_documento_cae_al_default() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));

            provider.transmit(documento(ElectronicDocumentType.FE_VENTA, clienteConDocumento(null),
                    null, null, null, ElectronicDocumentMother.BRANCH_ID,
                    ElectronicDocumentMother.unaLineaGravada(), BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, null, null), configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) body.get("customer");
            assertThat(customer.get("identity_document_id")).isEqualTo("1");
        }

        @Test
        @DisplayName("un adquiriente persona juridica manda type_organization_id 1")
        void adquiriente_persona_juridica() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
            CustomerSnapshot juridica = new CustomerSnapshot("NIT", "900999888", "1", "JURIDICA",
                    "Cliente SAS", "Cliente SAS", "compras@cliente.co", "05001",
                    TaxRegime.RESPONSABLE_IVA, FiscalResponsibility.NO_APLICA);

            provider.transmit(documento(ElectronicDocumentType.FE_VENTA, juridica, null, null, null,
                    ElectronicDocumentMother.BRANCH_ID, ElectronicDocumentMother.unaLineaGravada(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                    configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) body.get("customer");
            assertThat(customer.get("type_organization_id")).isEqualTo(1);
            assertThat(customer.get("company_name")).isEqualTo("Cliente SAS");
        }

        @Test
        @DisplayName("un adquiriente sin email no manda la clave email en el cuerpo")
        void adquiriente_sin_email_omite_la_clave() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
            CustomerSnapshot sinEmail = new CustomerSnapshot("CEDULA_CIUDADANIA", "1020304050", "3",
                    "NATURAL", null, "Ana Perez", null, "05001", TaxRegime.NO_RESPONSABLE_IVA,
                    FiscalResponsibility.NO_APLICA);

            provider.transmit(documento(ElectronicDocumentType.FE_VENTA, sinEmail, null, null, null,
                    ElectronicDocumentMother.BRANCH_ID, ElectronicDocumentMother.unaLineaGravada(),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                    configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) body.get("customer");
            assertThat(customer).doesNotContainKey("email");
        }

        @Test
        @DisplayName("una linea con impuesto al consumo (INC) usa el tax_id de INC, no el de IVA")
        void linea_inc_usa_su_tax_id() {
            stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
            List<ElectronicDocumentLine> lineaInc = List
                    .of(ElectronicDocumentMother.incLine(1, "1000.00", "8", "80.00"));

            provider.transmit(
                    documento(ElectronicDocumentType.FE_VENTA, ElectronicDocumentMother.customer(),
                            null, null, null, ElectronicDocumentMother.BRANCH_ID, lineaInc,
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                    configConLogin());

            Map<String, Object> body = capturarCuerpoEnviado();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lineas = (List<Map<String, Object>>) body.get("lines");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taxTotals = (List<Map<String, Object>>) lineas.get(0)
                    .get("tax_totals");
            assertThat(taxTotals).singleElement()
                    .satisfies(t -> assertThat(t.get("tax_id")).isEqualTo("4"));
        }

        @Test
        @DisplayName("un StatusMessage sin el patron LETRAS+numero no asigna prefix/consecutive")
        void status_message_sin_patron_no_asigna_numero() {
            stubTransmitResponse(200,
                    respuestaValidada(SELLO_VALIDO, "Documento autorizado sin numero reconocible"));

            ProviderResult result = provider.transmit(facturaBasica(), configConLogin());

            assertThat(result.status()).isEqualTo(DianStatus.VALIDADO);
            assertThat(result.prefix()).isNull();
            assertThat(result.consecutive()).isNull();
        }
    }

    @ParameterizedTest
    @CsvSource({"RESPONSABLE_IVA, 1", "NO_RESPONSABLE_IVA, 2"})
    @DisplayName("tax_regime_id refleja el regimen congelado del adquiriente")
    void tax_regime_id_refleja_el_regimen_del_adquiriente(TaxRegime regimen, int esperado) {
        montarCadenaRestClient();
        stubSinClaveTecnica();
        stubLogin("""
                {"access_token": "tok", "expires_at": "2099-01-01 00:00:00"}
                """);
        stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
        CustomerSnapshot cliente = new CustomerSnapshot("CEDULA_CIUDADANIA", "1020304050", "3",
                "NATURAL", null, "Ana Perez", "ana@correo.co", "05001", regimen,
                FiscalResponsibility.NO_APLICA);

        provider.transmit(documento(ElectronicDocumentType.FE_VENTA, cliente, null, null, null,
                ElectronicDocumentMother.BRANCH_ID, ElectronicDocumentMother.unaLineaGravada(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null), configConLogin());

        Map<String, Object> body = capturarCuerpoEnviado();
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) body.get("customer");
        assertThat(customer.get("tax_regime_id")).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource({"GRAN_CONTRIBUYENTE, 7", "AUTORRETENEDOR, 9", "AGENTE_RETENCION_IVA, 14",
            "REGIMEN_SIMPLE, 112", "NO_APLICA, 5"})
    @DisplayName("tax_level_id refleja la responsabilidad fiscal congelada del adquiriente")
    void tax_level_id_refleja_la_responsabilidad(FiscalResponsibility responsabilidad,
            int esperado) {
        montarCadenaRestClient();
        stubSinClaveTecnica();
        stubLogin("""
                {"access_token": "tok", "expires_at": "2099-01-01 00:00:00"}
                """);
        stubTransmitResponse(200, respuestaValidada(SELLO_VALIDO, "La Factura SETP1, ok."));
        CustomerSnapshot cliente = new CustomerSnapshot("CEDULA_CIUDADANIA", "1020304050", "3",
                "NATURAL", null, "Ana Perez", "ana@correo.co", "05001",
                TaxRegime.NO_RESPONSABLE_IVA, responsabilidad);

        provider.transmit(documento(ElectronicDocumentType.FE_VENTA, cliente, null, null, null,
                ElectronicDocumentMother.BRANCH_ID, ElectronicDocumentMother.unaLineaGravada(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null), configConLogin());

        Map<String, Object> body = capturarCuerpoEnviado();
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) body.get("customer");
        assertThat(customer.get("tax_level_id")).isEqualTo(esperado);
    }

    private static String respuestaValidada(String sello, String statusMessage) {
        return "{\"response\": {\"StatusCode\": \"00\", \"StatusMessage\": \"" + statusMessage
                + "\"}, \"XmlDocumentKey\": \"" + sello
                + "\", \"qr\": {\"qrDian\": \"QRDATA\", \"url\": \"https://qr\"}, "
                + "\"AttachedDocument\": {\"url\": \"https://xml\"}}";
    }
}
