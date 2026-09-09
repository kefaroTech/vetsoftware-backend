package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.paymentgateway.domain.GatewayTransactionStatus;
import com.vetsoftware.app.paymentgateway.domain.ParsedWompiEvent;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNotConfiguredException;
import com.vetsoftware.app.paymentgateway.domain.WompiMalformedEventException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Los valores del JSON oficial son los mismos que fija
 * {@code WompiSignaturesTest}: el checksum que trae este cuerpo se calculó una
 * vez con {@link WompiSignatures#eventChecksum} sobre esa cadena exacta.
 */
@DisplayName("WompiEventJsonAdapter")
class WompiEventJsonAdapterTest {

    private static final String EVENTS_SECRET = "prod_events_OcHnIzeBl5socpwByQ4hA52Em3USQ93Z";
    private static final String CHECKSUM_ESPERADO = "5a18ec5e8fdb7df463e9f94774cba8f583ba21bd04a09ceff2ea68a4bc0aefbe";

    private final WompiEventJsonAdapter adapter = new WompiEventJsonAdapter(new ObjectMapper(),
            propiedades(true));

    private static WompiProperties propiedades(boolean enabled) {
        return new WompiProperties(enabled, "https://sandbox.wompi.test/v1", "pub_test", "prv_test",
                "test_integrity", EVENTS_SECRET, 6, Duration.ofSeconds(2), Duration.ofHours(24),
                65536L, Duration.ofHours(24));
    }

    private static String eventoOficial(String propertiesJson) {
        return """
                {"event":"transaction.updated","data":{"transaction":{
                  "id":"1234-1610641025-49201","status":"APPROVED","status_message":null,
                  "amount_in_cents":4490000,"reference":"MZQ3X2DE2SM"
                }},"sent_at":"2021-01-14T15:37:05.836Z","timestamp":1530291411,
                "signature":{"properties":%s,"checksum":"%s"},"environment":"test"}
                """.formatted(propertiesJson, CHECKSUM_ESPERADO);
    }

    @Nested
    @DisplayName("parseo del evento oficial")
    class ParseoDelEventoOficial {

        @Test
        @DisplayName("interpreta event, data.transaction.* y timestamp")
        void interpreta_event_data_transaction_y_timestamp() {
            ParsedWompiEvent event = adapter.parse(eventoOficial(
                    "[\"transaction.id\",\"transaction.status\",\"transaction.amount_in_cents\"]"));

            assertThat(event.eventType()).isEqualTo("transaction.updated");
            assertThat(event.isTransactionUpdated()).isTrue();
            assertThat(event.transactionId()).isEqualTo("1234-1610641025-49201");
            assertThat(event.status()).isEqualTo(GatewayTransactionStatus.APPROVED);
            assertThat(event.statusMessage()).isNull();
            assertThat(event.timestamp()).isEqualTo(1530291411L);
            assertThat(event.amountInCents()).isEqualTo(4490000L);
        }

        @Test
        @DisplayName("calcula la concatenacion en el orden de signature.properties")
        void calcula_la_concatenacion_en_el_orden_de_properties() {
            ParsedWompiEvent event = adapter.parse(eventoOficial(
                    "[\"transaction.id\",\"transaction.status\",\"transaction.amount_in_cents\"]"));

            assertThat(event.checksumPropertyValues()).containsExactly("1234-1610641025-49201",
                    "APPROVED", "4490000");
        }

        @Test
        @DisplayName("un orden distinto de properties resuelve los valores en ese mismo orden")
        void un_orden_distinto_resuelve_los_valores_en_ese_orden() {
            ParsedWompiEvent event = adapter.parse(eventoOficial(
                    "[\"transaction.amount_in_cents\",\"transaction.id\",\"transaction.status\"]"));

            assertThat(event.checksumPropertyValues()).containsExactly("4490000",
                    "1234-1610641025-49201", "APPROVED");
        }

        @Test
        @DisplayName("un status_message presente se interpreta como texto")
        void un_status_message_presente_se_interpreta() {
            String raw = """
                    {"event":"transaction.updated","data":{"transaction":{
                      "id":"tx-1","status":"DECLINED","status_message":"Fondos insuficientes",
                      "amount_in_cents":1000,"reference":"REF-1"
                    }},"timestamp":1,"signature":{"properties":[],"checksum":"x"}}
                    """;

            ParsedWompiEvent event = adapter.parse(raw);

            assertThat(event.status()).isEqualTo(GatewayTransactionStatus.DECLINED);
            assertThat(event.statusMessage()).isEqualTo("Fondos insuficientes");
        }

        @Test
        @DisplayName("un evento distinto de transaction.updated no se marca como tal")
        void un_evento_distinto_no_se_marca_como_transaction_updated() {
            String raw = """
                    {"event":"payment_link.paid","data":{"transaction":{
                      "id":"tx-1","status":"APPROVED","amount_in_cents":1000,"reference":"REF-1"
                    }},"timestamp":1,"signature":{"properties":[],"checksum":"x"}}
                    """;

            ParsedWompiEvent event = adapter.parse(raw);

            assertThat(event.isTransactionUpdated()).isFalse();
        }

        @Test
        @DisplayName("un JSON malformado se traduce a WompiMalformedEventException")
        void un_json_malformado_se_traduce_a_wompi_malformed_event_exception() {
            assertThatThrownBy(() -> adapter.parse("{esto no es json"))
                    .isInstanceOf(WompiMalformedEventException.class)
                    .hasMessageContaining("webhook");
        }

        @Test
        @DisplayName("un JSON valido sin la clave event se rechaza en vez de devolver eventType nulo")
        void json_valido_sin_event_se_rechaza() {
            String raw = """
                    {"data":{"transaction":{
                      "id":"tx-1","status":"APPROVED","amount_in_cents":1000,"reference":"REF-1"
                    }},"timestamp":1,"signature":{"properties":[],"checksum":"x"}}
                    """;

            assertThatThrownBy(() -> adapter.parse(raw))
                    .isInstanceOf(WompiMalformedEventException.class);
        }

        @Test
        @DisplayName("un JSON valido sin data.transaction se rechaza")
        void json_valido_sin_transaction_se_rechaza() {
            String raw = """
                    {"event":"transaction.updated","data":{},"timestamp":1,
                     "signature":{"properties":[],"checksum":"x"}}
                    """;

            assertThatThrownBy(() -> adapter.parse(raw))
                    .isInstanceOf(WompiMalformedEventException.class);
        }

        @Test
        @DisplayName("un JSON valido sin transaction.id se rechaza")
        void json_valido_sin_transaction_id_se_rechaza() {
            String raw = """
                    {"event":"transaction.updated","data":{"transaction":{
                      "status":"APPROVED","amount_in_cents":1000,"reference":"REF-1"
                    }},"timestamp":1,"signature":{"properties":[],"checksum":"x"}}
                    """;

            assertThatThrownBy(() -> adapter.parse(raw))
                    .isInstanceOf(WompiMalformedEventException.class);
        }

        @Test
        @DisplayName("un JSON valido sin status se rechaza")
        void json_valido_sin_status_se_rechaza() {
            String raw = """
                    {"event":"transaction.updated","data":{"transaction":{
                      "id":"tx-1","amount_in_cents":1000,"reference":"REF-1"
                    }},"timestamp":1,"signature":{"properties":[],"checksum":"x"}}
                    """;

            assertThatThrownBy(() -> adapter.parse(raw))
                    .isInstanceOf(WompiMalformedEventException.class);
        }

        @Test
        @DisplayName("un JSON valido sin timestamp se rechaza")
        void json_valido_sin_timestamp_se_rechaza() {
            String raw = """
                    {"event":"transaction.updated","data":{"transaction":{
                      "id":"tx-1","status":"APPROVED","amount_in_cents":1000,"reference":"REF-1"
                    }},"signature":{"properties":[],"checksum":"x"}}
                    """;

            assertThatThrownBy(() -> adapter.parse(raw))
                    .isInstanceOf(WompiMalformedEventException.class);
        }
    }

    @Nested
    @DisplayName("matchesChecksum")
    class MatchesChecksum {

        @Test
        @DisplayName("acepta el checksum del ejemplo oficial")
        void acepta_el_checksum_del_ejemplo_oficial() {
            ParsedWompiEvent event = adapter.parse(eventoOficial(
                    "[\"transaction.id\",\"transaction.status\",\"transaction.amount_in_cents\"]"));

            assertThat(adapter.matchesChecksum(event, CHECKSUM_ESPERADO)).isTrue();
        }

        @Test
        @DisplayName("rechaza un checksum forjado")
        void rechaza_un_checksum_forjado() {
            ParsedWompiEvent event = adapter.parse(eventoOficial(
                    "[\"transaction.id\",\"transaction.status\",\"transaction.amount_in_cents\"]"));

            assertThat(adapter.matchesChecksum(event,
                    "0000000000000000000000000000000000000000000000000000000000000000")).isFalse();
        }

        @Test
        @DisplayName("un cambio en el orden de properties invalida el checksum calculado para el orden original")
        void un_cambio_de_orden_invalida_el_checksum_original() {
            List<String> valoresReordenados = List.of("4490000", "1234-1610641025-49201",
                    "APPROVED");
            ParsedWompiEvent event = new ParsedWompiEvent("transaction.updated",
                    "1234-1610641025-49201", GatewayTransactionStatus.APPROVED, null, 1530291411L,
                    4490000L, valoresReordenados);

            assertThat(adapter.matchesChecksum(event, CHECKSUM_ESPERADO)).isFalse();
        }
    }

    @Nested
    @DisplayName("computeChecksum")
    class ComputeChecksum {

        @Test
        @DisplayName("devuelve el mismo checksum que trae el ejemplo oficial")
        void devuelve_el_checksum_del_ejemplo_oficial() {
            ParsedWompiEvent event = adapter.parse(eventoOficial(
                    "[\"transaction.id\",\"transaction.status\",\"transaction.amount_in_cents\"]"));

            assertThat(adapter.computeChecksum(event)).isEqualTo(CHECKSUM_ESPERADO);
        }
    }

    @Nested
    @DisplayName("requireConfigured")
    class RequireConfigured {

        @Test
        @DisplayName("con Wompi deshabilitado lanza PaymentGatewayNotConfiguredException")
        void deshabilitado_lanza() {
            WompiEventJsonAdapter deshabilitado = new WompiEventJsonAdapter(new ObjectMapper(),
                    propiedades(false));

            assertThatThrownBy(deshabilitado::requireConfigured)
                    .isInstanceOf(PaymentGatewayNotConfiguredException.class);
        }

        @Test
        @DisplayName("habilitado y con secreto no lanza")
        void habilitado_con_secreto_no_lanza() {
            org.assertj.core.api.Assertions.assertThatCode(adapter::requireConfigured)
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("freshnessTolerance")
    class FreshnessTolerance {

        @Test
        @DisplayName("expone la tolerancia configurada en WompiProperties")
        void expone_la_tolerancia_configurada() {
            assertThat(adapter.freshnessTolerance()).isEqualTo(Duration.ofHours(24));
        }
    }
}
