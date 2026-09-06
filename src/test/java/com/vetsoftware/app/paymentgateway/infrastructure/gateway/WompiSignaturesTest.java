package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Los dos ejemplos oficiales de la documentación de Wompi: el hash esperado se
 * calculó una vez sobre la cadena concatenada exacta que la documentación
 * publica, y esta prueba fija ese resultado.
 */
@DisplayName("WompiSignatures")
class WompiSignaturesTest {

    @Nested
    @DisplayName("firma de integridad")
    class FirmaDeIntegridad {

        @Test
        @DisplayName("reproduce el ejemplo oficial de reference + amount_in_cents + currency + secret")
        void reproduce_el_ejemplo_oficial() {
            String signature = WompiSignatures.integritySignature("sk8-438k4-xmxm392-sn2m24",
                    90000L, "COP", "prod_integrity_Z5mMke9x0k8gpErbDqwrJXMqsI6SFli6");

            assertThat(signature)
                    .isEqualTo("37c8407747e595535433ef8f6a811d853cd943046624a0ec04662b17bbf33bf5");
        }

        @Test
        @DisplayName("es sensible al orden: currency antes que el secreto")
        void es_sensible_al_orden() {
            String signature = WompiSignatures.integritySignature("ref", 1000L, "COP", "secret");

            assertThat(signature).isNotEqualTo(
                    WompiSignatures.integritySignature("ref", 1000L, "secret", "COP"));
        }
    }

    @Nested
    @DisplayName("checksum de eventos")
    class ChecksumDeEventos {

        private static final List<String> PROPIEDADES = List.of("1234-1610641025-49201", "APPROVED",
                "4490000");
        private static final long TIMESTAMP = 1530291411L;
        private static final String SECRETO = "prod_events_OcHnIzeBl5socpwByQ4hA52Em3USQ93Z";
        private static final String CHECKSUM_ESPERADO = "5a18ec5e8fdb7df463e9f94774cba8f583ba21bd04a09ceff2ea68a4bc0aefbe";

        @Test
        @DisplayName("reproduce el ejemplo oficial de id + status + amount_in_cents + timestamp + secreto")
        void reproduce_el_ejemplo_oficial() {
            String checksum = WompiSignatures.eventChecksum(PROPIEDADES, TIMESTAMP, SECRETO);

            assertThat(checksum).isEqualTo(CHECKSUM_ESPERADO);
        }

        @Test
        @DisplayName("matchesEventChecksum acepta el checksum correcto")
        void matches_event_checksum_acepta_el_correcto() {
            assertThat(WompiSignatures.matchesEventChecksum(CHECKSUM_ESPERADO, PROPIEDADES,
                    TIMESTAMP, SECRETO)).isTrue();
        }

        @Test
        @DisplayName("matchesEventChecksum es insensible a mayúsculas del header")
        void matches_event_checksum_es_insensible_a_mayusculas() {
            assertThat(WompiSignatures.matchesEventChecksum(CHECKSUM_ESPERADO.toUpperCase(),
                    PROPIEDADES, TIMESTAMP, SECRETO)).isTrue();
        }

        @Test
        @DisplayName("matchesEventChecksum rechaza un checksum forjado")
        void matches_event_checksum_rechaza_uno_forjado() {
            assertThat(WompiSignatures.matchesEventChecksum("0000000000000000000000000000000",
                    PROPIEDADES, TIMESTAMP, SECRETO)).isFalse();
        }

        @Test
        @DisplayName("matchesEventChecksum rechaza un header nulo")
        void matches_event_checksum_rechaza_nulo() {
            assertThat(WompiSignatures.matchesEventChecksum(null, PROPIEDADES, TIMESTAMP, SECRETO))
                    .isFalse();
        }
    }
}
