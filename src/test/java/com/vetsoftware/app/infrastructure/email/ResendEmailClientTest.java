package com.vetsoftware.app.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * El transporte se corta con un interceptor del propio {@link RestClient}: la
 * petición se serializa de verdad (misma ruta que en producción) pero nunca
 * sale a la red, así que se puede afirmar sobre el JSON exacto que recibiría
 * Resend.
 */
@DisplayName("ResendEmailClient")
class ResendEmailClientTest {

    private static final String FROM = "clinica@vetsoftware.co";
    private static final String API_KEY = "re_una_clave";
    private static final String BASE_URL = "https://api.resend.test";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final List<SentRequest> sent = new ArrayList<>();
    private final CapturingHandler handler = new CapturingHandler();
    private final ObservationRegistry observationRegistry = registryWith(handler);

    @Nested
    @DisplayName("envío con HTML propio")
    class Send {

        @Test
        @DisplayName("publica el correo en /emails con la API key en la cabecera")
        void publica_el_correo_en_emails_con_la_api_key_en_la_cabecera() {
            ResendEmailClient client = clientRespondingWith(accepted());

            observing(() -> client.send("due@correo.co", "copia@correo.co", "Su factura",
                    "<p>Gracias</p>", List.of()));

            SentRequest request = sent.getFirst();
            assertThat(request.uri()).isEqualTo(BASE_URL + "/emails");
            assertThat(request.authorization()).isEqualTo("Bearer " + API_KEY);
            JsonNode body = request.json();
            assertThat(body.get("from").asString()).isEqualTo(FROM);
            assertThat(body.get("to").get(0).asString()).isEqualTo("due@correo.co");
            assertThat(body.get("cc").get(0).asString()).isEqualTo("copia@correo.co");
            assertThat(body.get("subject").asString()).isEqualTo("Su factura");
            assertThat(body.get("html").asString()).isEqualTo("<p>Gracias</p>");
            assertThat(body.has("attachments")).isFalse();
        }

        @Test
        @DisplayName("codifica los adjuntos en base64 conservando el nombre del archivo")
        void codifica_los_adjuntos_en_base64() {
            ResendEmailClient client = clientRespondingWith(accepted());
            byte[] pdf = "%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8);

            observing(() -> client.send("due@correo.co", null, "Su factura", "<p>Gracias</p>",
                    List.of(new ResendEmailClient.Attachment("factura.pdf", pdf))));

            JsonNode attachment = sent.getFirst().json().get("attachments").get(0);
            assertThat(attachment.get("filename").asString()).isEqualTo("factura.pdf");
            assertThat(attachment.get("content").asString())
                    .isEqualTo(Base64.getEncoder().encodeToString(pdf));
        }

        @Test
        @DisplayName("omite el cc y el asunto cuando llegan vacíos")
        void omite_el_cc_y_el_asunto_cuando_llegan_vacios() {
            ResendEmailClient client = clientRespondingWith(accepted());

            observing(() -> client.send("due@correo.co", "  ", "  ", "<p>Gracias</p>", null));

            JsonNode body = sent.getFirst().json();
            assertThat(body.has("cc")).isFalse();
            assertThat(body.has("subject")).isFalse();
        }

        @Test
        @DisplayName("marca la observación como exitosa cuando Resend acepta el envío")
        void marca_la_observacion_como_exitosa() {
            ResendEmailClient client = clientRespondingWith(accepted());

            Observation.Context context = observing(
                    () -> client.send("due@correo.co", null, "Su factura", "<p>Ok</p>", List.of()));

            assertThat(outcomeOf(context)).isEqualTo("success");
            assertThat(context.getError()).isNull();
        }
    }

    @Nested
    @DisplayName("envío con plantilla de Resend")
    class SendTemplate {

        @Test
        @DisplayName("envía el identificador de la plantilla junto a sus variables")
        void envia_el_identificador_de_la_plantilla_junto_a_sus_variables() {
            ResendEmailClient client = clientRespondingWith(accepted());

            observing(() -> client.sendTemplate("due@correo.co", null, null, "tpl_bienvenida",
                    Map.of("NOMBRE", "Ana")));

            JsonNode template = sent.getFirst().json().get("template");
            assertThat(template.get("id").asString()).isEqualTo("tpl_bienvenida");
            assertThat(template.get("variables").get("NOMBRE").asString()).isEqualTo("Ana");
            assertThat(sent.getFirst().json().has("subject")).isFalse();
        }

        @Test
        @DisplayName("envía un mapa vacío de variables cuando no se aportan")
        void envia_un_mapa_vacio_de_variables_cuando_no_se_aportan() {
            ResendEmailClient client = clientRespondingWith(accepted());

            observing(() -> client.sendTemplate("due@correo.co", null, "Bienvenida",
                    "tpl_bienvenida", null));

            assertThat(sent.getFirst().json().get("template").get("variables").isEmpty()).isTrue();
        }

        @Test
        @DisplayName("no llama a Resend si la plantilla no está configurada")
        void no_llama_a_resend_si_la_plantilla_no_esta_configurada() {
            ResendEmailClient client = clientRespondingWith(accepted());

            Observation.Context context = observing(
                    () -> client.sendTemplate("due@correo.co", null, "Bienvenida", "  ", Map.of()));

            assertThat(sent).isEmpty();
            assertThat(outcomeOf(context)).isEqualTo("invalid");
        }
    }

    @Nested
    @DisplayName("cuando no debe enviarse nada")
    class NoEnvia {

        @Test
        @DisplayName("no envía ni falla cuando el correo está deshabilitado")
        void no_envia_ni_falla_cuando_el_correo_esta_deshabilitado() {
            ResendEmailClient client = new ResendEmailClient(false, FROM, API_KEY, BASE_URL,
                    recordingBuilder(accepted()), observationRegistry);

            Observation.Context context = observing(
                    () -> client.send("due@correo.co", null, "Su factura", "<p>Ok</p>", List.of()));

            assertThat(client.isEnabled()).isFalse();
            assertThat(sent).isEmpty();
            assertThat(outcomeOf(context)).isEqualTo("skipped");
        }

        @Test
        @DisplayName("no envía cuando falta el destinatario")
        void no_envia_cuando_falta_el_destinatario() {
            ResendEmailClient client = clientRespondingWith(accepted());

            Observation.Context context = observing(
                    () -> client.send("  ", null, "Su factura", "<p>Ok</p>", List.of()));

            assertThat(sent).isEmpty();
            assertThat(outcomeOf(context)).isEqualTo("invalid");
        }

        @Test
        @DisplayName("no envía cuando la API key de Resend no está configurada")
        void no_envia_cuando_la_api_key_no_esta_configurada() {
            ResendEmailClient client = new ResendEmailClient(true, FROM, "", BASE_URL,
                    recordingBuilder(accepted()), observationRegistry);

            Observation.Context context = observing(
                    () -> client.send("due@correo.co", null, "Su factura", "<p>Ok</p>", List.of()));

            assertThat(sent).isEmpty();
            assertThat(outcomeOf(context)).isEqualTo("misconfigured");
        }
    }

    @Nested
    @DisplayName("fallos del proveedor")
    class Fallos {

        @Test
        @DisplayName("no propaga el error cuando Resend responde con estado de error")
        void no_propaga_el_error_cuando_resend_responde_con_estado_de_error() {
            ResendEmailClient client = clientRespondingWith(() -> new MockClientHttpResponse(
                    "{\"message\":\"API key inválida\"}".getBytes(StandardCharsets.UTF_8),
                    HttpStatus.UNAUTHORIZED));

            Observation.Context context = observing(() -> assertThatCode(
                    () -> client.send("due@correo.co", null, "Su factura", "<p>Ok</p>", List.of()))
                    .doesNotThrowAnyException());

            assertThat(sent).hasSize(1);
            assertThat(outcomeOf(context)).isEqualTo("failure");
            assertThat(context.getError()).isInstanceOf(RestClientResponseException.class);
        }

        @Test
        @DisplayName("no propaga el error cuando la red falla")
        void no_propaga_el_error_cuando_la_red_falla() {
            ResendEmailClient client = clientRespondingWith(() -> {
                throw new IOException("conexión reiniciada");
            });

            Observation.Context context = observing(
                    () -> assertThatCode(() -> client.sendTemplate("due@correo.co", null,
                            "Bienvenida", "tpl_1", Map.of())).doesNotThrowAnyException());

            assertThat(outcomeOf(context)).isEqualTo("failure");
            assertThat(context.getError()).isInstanceOf(ResourceAccessException.class);
        }
    }

    @Nested
    @DisplayName("sin una observación activa (fuera de un @Observed real)")
    class SinObservacionActiva {

        @Test
        @DisplayName("un envío exitoso no falla al intentar anotar una observación inexistente")
        void un_envio_exitoso_no_falla_sin_observacion_activa() {
            ResendEmailClient client = clientRespondingWith(accepted());

            assertThatCode(
                    () -> client.send("due@correo.co", null, "Su factura", "<p>Ok</p>", List.of()))
                    .doesNotThrowAnyException();

            assertThat(sent).hasSize(1);
        }

        @Test
        @DisplayName("un fallo del proveedor no falla al intentar anotar el error en una "
                + "observación inexistente")
        void un_fallo_del_proveedor_no_falla_sin_observacion_activa() {
            ResendEmailClient client = clientRespondingWith(() -> new MockClientHttpResponse(
                    "{\"message\":\"error\"}".getBytes(StandardCharsets.UTF_8),
                    HttpStatus.INTERNAL_SERVER_ERROR));

            assertThatCode(
                    () -> client.send("due@correo.co", null, "Su factura", "<p>Ok</p>", List.of()))
                    .doesNotThrowAnyException();

            assertThat(sent).hasSize(1);
        }
    }

    private ResendEmailClient clientRespondingWith(Responder responder) {
        return new ResendEmailClient(true, FROM, API_KEY, BASE_URL, recordingBuilder(responder),
                observationRegistry);
    }

    private RestClient.Builder recordingBuilder(Responder responder) {
        return RestClient.builder().requestInterceptor((request, body, execution) -> {
            sent.add(new SentRequest(request.getURI().toString(),
                    request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION),
                    new String(body, StandardCharsets.UTF_8)));
            return responder.respond();
        });
    }

    private static Responder accepted() {
        return () -> new MockClientHttpResponse(new byte[0], HttpStatus.OK);
    }

    /**
     * Ejecuta la acción dentro de una observación activa, que es lo que en
     * producción crea {@code @Observed}, y devuelve su contexto ya cerrado.
     */
    private Observation.Context observing(Runnable action) {
        Observation observation = Observation.start("email.send", observationRegistry);
        try (Observation.Scope ignored = observation.openScope()) {
            action.run();
        } finally {
            observation.stop();
        }
        return handler.stopped.getLast();
    }

    private static String outcomeOf(Observation.Context context) {
        return context.getLowCardinalityKeyValue("email.outcome").getValue();
    }

    private static ObservationRegistry registryWith(CapturingHandler handler) {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(handler);
        return registry;
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

    private static final class CapturingHandler implements ObservationHandler<Observation.Context> {

        private final List<Observation.Context> stopped = new ArrayList<>();

        @Override
        public void onStop(Observation.Context context) {
            stopped.add(context);
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }
}
