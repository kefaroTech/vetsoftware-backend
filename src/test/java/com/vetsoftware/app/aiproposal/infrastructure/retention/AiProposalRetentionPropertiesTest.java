package com.vetsoftware.app.aiproposal.infrastructure.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Los plazos de retencion son configuracion, y por eso hay que validarlos.
 *
 * <p>
 * <b>Un plazo mal escrito en el entorno no puede descubrirse a las 03:55.</b>
 * De un borrado no se vuelve: la validacion corre al construir el job, es decir
 * al arrancar la aplicacion, para que un valor imposible impida el despliegue
 * en vez de vaciar la tabla.
 */
@DisplayName("AiProposalRetentionProperties — los plazos son configuracion, y se validan")
class AiProposalRetentionPropertiesTest {

    private static AiProposalRetentionProperties porDefecto() {
        return new AiProposalRetentionProperties();
    }

    @Nested
    @DisplayName("Valores por defecto")
    class ValoresPorDefecto {

        /**
         * Son la propuesta de ingenieria, no una decision juridica cerrada: R-5 sigue
         * abierto. Estan aqui como <b>defecto sobrescribible</b>, que es justo lo que
         * una constante no seria.
         */
        @Test
        @DisplayName("90 dias para anonimizar y 24 meses para purgar, ambos sobrescribibles")
        void los_plazos_propuestos() {
            AiProposalRetentionProperties propiedades = porDefecto();

            assertThat(propiedades.getAnonymizeAfter()).isEqualTo(Duration.ofDays(90));
            assertThat(propiedades.getPurgeAfter()).isEqualTo(Duration.ofDays(730));
            assertThat(propiedades.isEnabled()).isTrue();

            propiedades.setAnonymizeAfter(Duration.ofDays(45));
            assertThat(propiedades.getAnonymizeAfter()).isEqualTo(Duration.ofDays(45));
        }

        @Test
        @DisplayName("el lote y el tope de lotes tambien se configuran")
        void el_lote_se_configura() {
            AiProposalRetentionProperties propiedades = porDefecto();

            assertThat(propiedades.getBatchSize()).isEqualTo(500);
            assertThat(propiedades.getMaxBatchesPerRun()).isEqualTo(20);

            propiedades.setBatchSize(1_000);
            propiedades.setMaxBatchesPerRun(5);
            propiedades.setEnabled(false);

            assertThat(propiedades.getBatchSize()).isEqualTo(1_000);
            assertThat(propiedades.getMaxBatchesPerRun()).isEqualTo(5);
            assertThat(propiedades.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("lo que viene por defecto es valido")
        void lo_de_serie_es_valido() {
            porDefecto().validate();
        }

        @Test
        @DisplayName("un plazo de anonimizacion nulo o negativo no arranca")
        void plazo_de_anonimizacion_invalido() {
            AiProposalRetentionProperties propiedades = porDefecto();
            propiedades.setAnonymizeAfter(Duration.ZERO);

            assertThatThrownBy(propiedades::validate).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("anonymize-after");
        }

        @Test
        @DisplayName("un plazo de purga nulo o negativo no arranca")
        void plazo_de_purga_invalido() {
            AiProposalRetentionProperties propiedades = porDefecto();
            propiedades.setPurgeAfter(Duration.ofDays(-1));

            assertThatThrownBy(propiedades::validate).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("purge-after");
        }

        @Test
        @DisplayName("un lote fuera de rango no arranca")
        void lote_fuera_de_rango() {
            AiProposalRetentionProperties propiedades = porDefecto();
            propiedades.setBatchSize(0);

            assertThatThrownBy(propiedades::validate).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("batch-size");
        }

        @Test
        @DisplayName("un tope de lotes fuera de rango no arranca")
        void tope_de_lotes_fuera_de_rango() {
            AiProposalRetentionProperties propiedades = porDefecto();
            propiedades.setMaxBatchesPerRun(500);

            assertThatThrownBy(propiedades::validate).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("max-batches-per-run");
        }

        /**
         * &#9940; El unico error de configuracion de esta clase que <b>destruye
         * datos</b>: con la purga por debajo de la anonimizacion, el barrido borraria
         * propuestas frescas —las que todavia no ha anonimizado— y lo haria en
         * silencio, informando exito.
         */
        @Test
        @DisplayName("purgar antes de anonimizar no arranca: borraria propuestas frescas")
        void purgar_antes_de_anonimizar_no_arranca() {
            AiProposalRetentionProperties propiedades = porDefecto();
            propiedades.setAnonymizeAfter(Duration.ofDays(90));
            propiedades.setPurgeAfter(Duration.ofDays(30));

            assertThatThrownBy(propiedades::validate).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("purge-after");
        }

        @Test
        @DisplayName("purgar exactamente cuando se anonimiza si es valido, aunque sea inutil")
        void purgar_a_la_vez_es_valido() {
            AiProposalRetentionProperties propiedades = porDefecto();
            propiedades.setAnonymizeAfter(Duration.ofDays(90));
            propiedades.setPurgeAfter(Duration.ofDays(90));

            propiedades.validate();
        }
    }
}
