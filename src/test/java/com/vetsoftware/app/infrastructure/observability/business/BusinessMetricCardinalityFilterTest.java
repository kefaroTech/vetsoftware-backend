package com.vetsoftware.app.infrastructure.observability.business;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BusinessMetricCardinalityFilterTest {

    private final BusinessMetricCardinalityFilter filter = new BusinessMetricCardinalityFilter();

    @Nested
    @DisplayName("métricas fuera del prefijo de negocio")
    class FueraDePrefijo {

        @Test
        @DisplayName("no decide sobre métricas ajenas al prefijo vetsoftware.business.")
        void es_neutral_para_metricas_fuera_del_prefijo() {
            Meter.Id id = new Meter.Id("jvm.memory.used", Tags.empty(), null, null,
                    Meter.Type.GAUGE);

            assertThat(filter.accept(id)).isEqualTo(MeterFilterReply.NEUTRAL);
        }
    }

    @Nested
    @DisplayName("etiquetas comunes de infraestructura")
    class EtiquetasComunes {

        @Test
        @DisplayName("deja pasar las etiquetas comunes sin comprobarlas contra la lista de valores permitidos")
        void es_neutral_cuando_solo_trae_etiquetas_comunes() {
            Meter.Id id = new Meter.Id(BusinessMetricNames.SALES_OPERATIONS,
                    Tags.of("application", "vetsoftware", "instance", "app-1", "environment", "dev",
                            "region", "us-east-1", "service", "backend"),
                    null, null, Meter.Type.COUNTER);

            assertThat(filter.accept(id)).isEqualTo(MeterFilterReply.NEUTRAL);
        }
    }

    @Nested
    @DisplayName("etiquetas de negocio")
    class EtiquetasDeNegocio {

        @Test
        @DisplayName("deja pasar un valor declarado en la lista blanca")
        void es_neutral_cuando_el_valor_esta_en_la_lista_blanca() {
            Meter.Id id = new Meter.Id(BusinessMetricNames.SALES_OPERATIONS,
                    Tags.of("result", "completed"), null, null, Meter.Type.COUNTER);

            assertThat(filter.accept(id)).isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @Test
        @DisplayName("deniega un valor de una etiqueta declarada que no está en la lista blanca")
        void deniega_un_valor_fuera_de_la_lista_blanca() {
            Meter.Id id = new Meter.Id(BusinessMetricNames.SALES_OPERATIONS,
                    Tags.of("result", "customer-provided"), null, null, Meter.Type.COUNTER);

            assertThat(filter.accept(id)).isEqualTo(MeterFilterReply.DENY);
        }

        @Test
        @DisplayName("deniega una etiqueta que no está declarada en absoluto")
        void deniega_una_etiqueta_no_declarada() {
            Meter.Id id = new Meter.Id(BusinessMetricNames.SALES_OPERATIONS,
                    Tags.of("companyId", "3"), null, null, Meter.Type.COUNTER);

            assertThat(filter.accept(id)).isEqualTo(MeterFilterReply.DENY);
        }
    }
}
