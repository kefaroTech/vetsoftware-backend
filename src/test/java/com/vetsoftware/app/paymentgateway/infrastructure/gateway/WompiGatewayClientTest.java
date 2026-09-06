package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.paymentgateway.domain.ChargeRequest;
import com.vetsoftware.app.paymentgateway.domain.CreatePaymentSourceRequest;
import com.vetsoftware.app.paymentgateway.domain.GatewayPaymentSource;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransaction;
import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.MerchantAcceptance;
import com.vetsoftware.app.paymentgateway.domain.WompiGatewayException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * El transporte se corta con un interceptor del propio {@link RestClient}, como
 * en {@code ResendEmailClientTest}: la petición se serializa de verdad pero
 * nunca sale a la red, así que se puede afirmar sobre el JSON exacto que
 * recibiría Wompi.
 */
@DisplayName("WompiGatewayClient")
class WompiGatewayClientTest {

    private static final String BASE_URL = "https://sandbox.wompi.test/v1";
    private static final String PUBLIC_KEY = "pub_test_12345";
    private static final String PRIVATE_KEY = "prv_test_67890";
    private static final String INTEGRITY_SECRET = "test_integrity_un_secreto";
    private static final String EVENTS_SECRET = "test_events_otro_secreto";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final List<SentRequest> sent = new ArrayList<>();

    @Nested
    @DisplayName("fetchAcceptance")
    class FetchAcceptance {

        @Test
        @DisplayName("parsea los dos tokens presignados y sus permalinks, con el Bearer de la llave publica")
        void parsea_los_dos_tokens_presignados() {
            WompiGatewayClient client = clientRespondingWith(
                    okJson("""
                            {"data":{
                              "presigned_acceptance":{"acceptance_token":"tok-eula","permalink":"https://wompi.test/eula"},
                              "presigned_personal_data_auth":{"acceptance_token":"tok-pda","permalink":"https://wompi.test/pda"}
                            }}
                            """));

            MerchantAcceptance acceptance = client.fetchAcceptance();

            assertThat(acceptance).isEqualTo(new MerchantAcceptance("tok-eula",
                    "https://wompi.test/eula", "tok-pda", "https://wompi.test/pda"));
            assertThat(sent.getFirst().uri()).isEqualTo(BASE_URL + "/merchants/" + PUBLIC_KEY);
            assertThat(sent.getFirst().authorization()).isEqualTo("Bearer " + PUBLIC_KEY);
        }
    }

    @Nested
    @DisplayName("createPaymentSource")
    class CreatePaymentSource {

        @Test
        @DisplayName("manda type CARD y el Bearer de la llave privada, y parsea data.id")
        void manda_el_cuerpo_correcto_y_parsea_data_id() {
            WompiGatewayClient client = clientRespondingWith(okJson(
                    "{\"data\":{\"id\":987654,\"type\":\"CARD\",\"status\":\"AVAILABLE\"}}"));
            CreatePaymentSourceRequest request = new CreatePaymentSourceRequest("tok_test_card",
                    "cliente@correo.co", "tok-eula", true);

            GatewayPaymentSource source = client.createPaymentSource(request);

            assertThat(source).isEqualTo(new GatewayPaymentSource(987654L, "AVAILABLE"));
            assertThat(sent.getFirst().uri()).isEqualTo(BASE_URL + "/payment_sources");
            assertThat(sent.getFirst().authorization()).isEqualTo("Bearer " + PRIVATE_KEY);
            JsonNode body = sent.getFirst().json();
            assertThat(body.get("type").asString()).isEqualTo("CARD");
            assertThat(body.get("token").asString()).isEqualTo("tok_test_card");
            assertThat(body.get("customer_email").asString()).isEqualTo("cliente@correo.co");
            assertThat(body.get("acceptance_token").asString()).isEqualTo("tok-eula");
            assertThat(body.get("accept_personal_auth").asBoolean()).isTrue();
        }
    }

    @Nested
    @DisplayName("charge")
    class Charge {

        @Test
        @DisplayName("firma con el secreto de integridad y manda un solo pago (installments = 1)")
        void firma_y_manda_los_campos_de_la_transaccion() {
            WompiGatewayClient client = clientRespondingWith(okJson(
                    "{\"data\":{\"id\":\"1234-tx\",\"status\":\"PENDING\",\"status_message\":null,"
                            + "\"reference\":\"VS-REF-P1\",\"amount_in_cents\":4490000,"
                            + "\"payment_method_type\":\"CARD\"}}"));
            ChargeRequest request = new ChargeRequest(987654L, 4490000L, "COP", "VS-REF-P1",
                    "cliente@correo.co");

            GatewayTransaction transaction = client.charge(request);

            assertThat(transaction.id()).isEqualTo("1234-tx");
            assertThat(transaction.status()).isEqualTo(GatewayTransactionStatus.PENDING);
            assertThat(sent.getFirst().uri()).isEqualTo(BASE_URL + "/transactions");
            assertThat(sent.getFirst().authorization()).isEqualTo("Bearer " + PRIVATE_KEY);
            JsonNode body = sent.getFirst().json();
            assertThat(body.get("amount_in_cents").asLong()).isEqualTo(4490000L);
            assertThat(body.get("currency").asString()).isEqualTo("COP");
            assertThat(body.get("reference").asString()).isEqualTo("VS-REF-P1");
            assertThat(body.get("payment_source_id").asLong()).isEqualTo(987654L);
            assertThat(body.get("customer_email").asString()).isEqualTo("cliente@correo.co");
            assertThat(body.get("payment_method").get("installments").asInt()).isEqualTo(1);
            assertThat(body.get("signature").asString()).isEqualTo(WompiSignatures
                    .integritySignature("VS-REF-P1", 4490000L, "COP", INTEGRITY_SECRET));
        }
    }

    @Nested
    @DisplayName("findTransaction")
    class FindTransaction {

        @Test
        @DisplayName("parsea el estado final y el motivo crudo del rechazo")
        void parsea_el_estado_final_y_el_motivo_del_rechazo() {
            WompiGatewayClient client = clientRespondingWith(
                    okJson("{\"data\":{\"id\":\"1234-tx\",\"status\":\"DECLINED\","
                            + "\"status_message\":\"Fondos insuficientes\","
                            + "\"reference\":\"VS-REF-P1\",\"amount_in_cents\":4490000,"
                            + "\"payment_method_type\":\"CARD\"}}"));

            GatewayTransaction transaction = client.findTransaction("1234-tx");

            assertThat(transaction.status()).isEqualTo(GatewayTransactionStatus.DECLINED);
            assertThat(transaction.statusMessage()).isEqualTo("Fondos insuficientes");
            assertThat(sent.getFirst().uri()).isEqualTo(BASE_URL + "/transactions/1234-tx");
            assertThat(sent.getFirst().authorization()).isEqualTo("Bearer " + PRIVATE_KEY);
        }
    }

    @Nested
    @DisplayName("fallos de Wompi")
    class Fallos {

        @Test
        @DisplayName("un 401 de Wompi se traduce a WompiGatewayException")
        void un_401_se_traduce_a_wompi_gateway_exception() {
            WompiGatewayClient client = clientRespondingWith(errorStatus(HttpStatus.UNAUTHORIZED,
                    "{\"error\":{\"type\":\"UNAUTHORIZED\"}}"));

            assertThatThrownBy(client::fetchAcceptance).isInstanceOf(WompiGatewayException.class)
                    .hasMessageContaining("aceptación").cause()
                    .isInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("un 500 de Wompi tambien se traduce, sin dejar escapar RestClientException")
        void un_500_se_traduce_a_wompi_gateway_exception() {
            WompiGatewayClient client = clientRespondingWith(errorStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR, "{\"error\":{\"type\":\"INTERNAL\"}}"));
            ChargeRequest request = new ChargeRequest(987654L, 4490000L, "COP", "VS-REF-P1",
                    "cliente@correo.co");

            assertThatThrownBy(() -> client.charge(request))
                    .isInstanceOf(WompiGatewayException.class).cause()
                    .isInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("un timeout de red tambien se traduce, sin escapar RestClientException")
        void un_timeout_se_traduce_a_wompi_gateway_exception() {
            WompiGatewayClient client = clientRespondingWith(() -> {
                throw new SocketTimeoutException("Read timed out");
            });

            assertThatThrownBy(client::fetchAcceptance).isInstanceOf(WompiGatewayException.class)
                    .cause().isInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("el mensaje de la excepcion nunca lleva la llave privada, el secreto ni el testigo de tarjeta")
        void el_mensaje_nunca_lleva_secretos() {
            WompiGatewayClient client = clientRespondingWith(errorStatus(HttpStatus.UNAUTHORIZED,
                    "{\"error\":{\"type\":\"UNAUTHORIZED\"}}"));
            CreatePaymentSourceRequest request = new CreatePaymentSourceRequest("tok_super_secreto",
                    "cliente@correo.co", "tok-eula", true);

            assertThatThrownBy(() -> client.createPaymentSource(request))
                    .isInstanceOf(WompiGatewayException.class).hasMessageNotContaining(PRIVATE_KEY)
                    .hasMessageNotContaining(INTEGRITY_SECRET)
                    .hasMessageNotContaining("tok_super_secreto");
        }
    }

    private WompiGatewayClient clientRespondingWith(Responder responder) {
        return new WompiGatewayClient(recordingClient(responder), defaultProperties());
    }

    private RestClient recordingClient(Responder responder) {
        return RestClient.builder().requestInterceptor((request, body, execution) -> {
            sent.add(new SentRequest(request.getURI().toString(),
                    request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION),
                    new String(body, StandardCharsets.UTF_8)));
            return responder.respond();
        }).build();
    }

    private static WompiProperties defaultProperties() {
        return new WompiProperties(true, BASE_URL, PUBLIC_KEY, PRIVATE_KEY, INTEGRITY_SECRET,
                EVENTS_SECRET, 6, Duration.ofSeconds(2));
    }

    private static Responder okJson(String json) {
        return () -> jsonResponse(json, HttpStatus.OK);
    }

    private static Responder errorStatus(HttpStatus status, String json) {
        return () -> jsonResponse(json, status);
    }

    // Content-Type explicito: sin el, MockClientHttpResponse cae en
    // application/octet-stream y RestClient no encuentra un HttpMessageConverter
    // para el cuerpo JSON (UnknownContentTypeException).
    private static MockClientHttpResponse jsonResponse(String json, HttpStatus status) {
        MockClientHttpResponse response = new MockClientHttpResponse(
                json.getBytes(StandardCharsets.UTF_8), status);
        response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return response;
    }

    @FunctionalInterface
    private interface Responder {
        ClientHttpResponse respond() throws IOException;
    }

    private record SentRequest(String uri, String authorization, String body) {

        JsonNode json() {
            return JSON.readTree(body);
        }
    }
}
