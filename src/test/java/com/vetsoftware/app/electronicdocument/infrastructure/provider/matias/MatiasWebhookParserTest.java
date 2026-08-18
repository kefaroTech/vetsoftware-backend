package com.vetsoftware.app.electronicdocument.infrastructure.provider.matias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.electronicdocument.application.port.out.ParsedWebhook;
import com.vetsoftware.app.electronicdocument.domain.WebhookOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.ObjectMapper;

@DisplayName("MatiasWebhookParser — HMAC + payload del webhook de MATIAS")
class MatiasWebhookParserTest {

    private final MatiasWebhookParser parser = new MatiasWebhookParser(new ObjectMapper());

    @Test
    @DisplayName("providerName es MATIAS")
    void provider_name_es_matias() {
        assertThat(parser.providerName()).isEqualTo("MATIAS");
    }

    @Nested
    @DisplayName("verifySignature — HMAC-SHA256")
    class VerifySignature {

        @Test
        @DisplayName("una firma valida con el prefijo sha256= se acepta")
        void firma_valida_con_prefijo_se_acepta() {
            String body = "{\"event\":\"document.accepted\"}";
            String secret = "un-secreto";
            String firma = "sha256=" + hmac(body, secret);

            assertThat(parser.verifySignature(body, firma, secret)).isTrue();
        }

        @Test
        @DisplayName("una firma valida sin el prefijo tambien se acepta")
        void firma_valida_sin_prefijo_se_acepta() {
            String body = "{\"event\":\"document.accepted\"}";
            String secret = "un-secreto";

            assertThat(parser.verifySignature(body, hmac(body, secret), secret)).isTrue();
        }

        @Test
        @DisplayName("una firma que no coincide se rechaza")
        void firma_que_no_coincide_se_rechaza() {
            assertThat(parser.verifySignature("{}", "sha256=deadbeef", "un-secreto")).isFalse();
        }

        @Test
        @DisplayName("sin cabecera de firma se rechaza sin lanzar")
        void sin_cabecera_de_firma_se_rechaza() {
            assertThat(parser.verifySignature("{}", null, "un-secreto")).isFalse();
        }

        @Test
        @DisplayName("sin secreto configurado se rechaza sin lanzar")
        void sin_secreto_configurado_se_rechaza() {
            assertThat(parser.verifySignature("{}", "sha256=abc", null)).isFalse();
        }

        private String hmac(String body, String secret) {
            try {
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                mac.init(new javax.crypto.spec.SecretKeySpec(
                        secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
                return java.util.HexFormat.of().formatHex(
                        mac.doFinal(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Nested
    @DisplayName("parse — traduce el payload a ParsedWebhook")
    class Parse {

        @Test
        @DisplayName("un evento document.accepted con track_id y sellos se parsea completo")
        void evento_accepted_completo() {
            String body = """
                    {"event": "document.accepted",
                     "data": {"track_id": "KEY-1", "prefix": "SETP", "consecutive": "990",
                               "cufe": "CUFE-1", "cude": null, "uuid": "uuid-1", "xml": "<xml/>",
                               "qr": "qr-data", "qr_url": "https://qr", "pdf_url": "https://pdf"}}
                    """;

            ParsedWebhook result = parser.parse(body);

            assertThat(result).isEqualTo(new ParsedWebhook(WebhookOutcome.ACCEPTED, "KEY-1", "SETP",
                    990L, "CUFE-1", null, "uuid-1", "<xml/>", "qr-data", "https://qr",
                    "https://pdf", null));
        }

        @Test
        @DisplayName("un evento document.rejected trae el motivo de rechazo")
        void evento_rejected_trae_motivo() {
            String body = """
                    {"event": "document.rejected",
                     "data": {"track_id": "KEY-2", "message": "firma invalida"}}
                    """;

            ParsedWebhook result = parser.parse(body);

            assertThat(result.outcome()).isEqualTo(WebhookOutcome.REJECTED);
            assertThat(result.rejectionReason()).isEqualTo("firma invalida");
        }

        @Test
        @DisplayName("un evento irrelevante (document.created) se parsea como IGNORED")
        void evento_irrelevante_es_ignored() {
            String body = """
                    {"event": "document.created", "data": {"track_id": "KEY-3"}}
                    """;

            assertThat(parser.parse(body).outcome()).isEqualTo(WebhookOutcome.IGNORED);
        }

        @Test
        @DisplayName("sin evento (null) se parsea como IGNORED")
        void sin_evento_es_ignored() {
            String body = """
                    {"data": {"track_id": "KEY-4"}}
                    """;

            assertThat(parser.parse(body).outcome()).isEqualTo(WebhookOutcome.IGNORED);
        }

        @Test
        @DisplayName("la clave prioriza track_id sobre document_key, id y uuid")
        void la_clave_prioriza_track_id() {
            String body = """
                    {"event": "document.accepted",
                     "data": {"document_key": "OTRA", "id": "OTRA-2", "uuid": "OTRA-3"}}
                    """;

            // Sin track_id, cae al siguiente disponible en orden.
            assertThat(parser.parse(body).providerDocumentKey()).isEqualTo("OTRA");
        }

        @Test
        @DisplayName("qr_url cae a public_url cuando no viene qr_url")
        void qr_url_cae_a_public_url() {
            String body = """
                    {"event": "document.accepted",
                     "data": {"track_id": "KEY-5", "public_url": "https://public"}}
                    """;

            assertThat(parser.parse(body).qrUrl()).isEqualTo("https://public");
        }

        @Test
        @DisplayName("error cae de message a error cuando no viene message")
        void mensaje_cae_a_error() {
            String body = """
                    {"event": "document.rejected",
                     "data": {"track_id": "KEY-6", "error": "documento duplicado"}}
                    """;

            assertThat(parser.parse(body).rejectionReason()).isEqualTo("documento duplicado");
        }

        @Test
        @DisplayName("consecutive cae a number cuando no viene consecutive")
        void consecutive_cae_a_number() {
            String body = """
                    {"event": "document.accepted",
                     "data": {"track_id": "KEY-7", "number": "abc123"}}
                    """;

            // parseLong limpia todo lo que no sea digito antes de parsear.
            assertThat(parser.parse(body).consecutive()).isEqualTo(123L);
        }

        @Test
        @DisplayName("un consecutivo no numerico (tras limpiar) se deja en null en vez de lanzar")
        void consecutivo_no_numerico_se_deja_null() {
            String body = """
                    {"event": "document.accepted",
                     "data": {"track_id": "KEY-8", "consecutive": "abc"}}
                    """;

            assertThat(parser.parse(body).consecutive()).isNull();
        }

        @Test
        @DisplayName("un payload sin data no falla: los campos quedan null")
        void payload_sin_data_no_falla() {
            String body = """
                    {"event": "document.accepted"}
                    """;

            ParsedWebhook result = parser.parse(body);

            assertThat(result.providerDocumentKey()).isNull();
            assertThat(result.cufe()).isNull();
        }

        @Test
        @DisplayName("un cuerpo que no es JSON valido lanza IllegalArgumentException")
        void cuerpo_invalido_lanza_illegal_argument() {
            assertThatThrownBy(() -> parser.parse("no-es-json"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Webhook MATIAS inválido");
        }
    }

    @ParameterizedTest
    @CsvSource({"document.ACCEPTED, ACCEPTED", "Document.Rejected, REJECTED",
            "document.voided, IGNORED"})
    @DisplayName("el evento se reconoce sin importar mayusculas/minusculas")
    void el_evento_se_reconoce_sin_importar_mayusculas(String event, WebhookOutcome esperado) {
        String body = "{\"event\": \"" + event + "\", \"data\": {\"track_id\": \"K\"}}";

        assertThat(parser.parse(body).outcome()).isEqualTo(esperado);
    }
}
