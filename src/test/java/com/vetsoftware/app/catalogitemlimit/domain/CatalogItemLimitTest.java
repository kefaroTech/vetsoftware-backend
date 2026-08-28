package com.vetsoftware.app.catalogitemlimit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CatalogItemLimit — el techo de fábrica de un artículo")
class CatalogItemLimitTest {

    private static final Long HISTORIA_CLINICA = 3L;
    private static final Long EJE_ANIMAL = 1L;
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 9, 1, 8, 0);

    private static CatalogItemLimit cienMascotas() {
        return CatalogItemLimit.create(HISTORIA_CLINICA, EJE_ANIMAL, MeasureKind.CUMULATIVE,
                LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK, null, 80, LimitMode.FULL,
                null, CREADO);
    }

    @Nested
    @DisplayName("R-LIMIT-33 · no cabe un techo a medio declarar")
    class ModoYCantidad {

        @Test
        @DisplayName("declarar LIMITED sin cantidad se rechaza")
        void declarar_LIMITED_sin_cantidad_se_rechaza() {
            assertThatThrownBy(() -> CatalogItemLimit.create(HISTORIA_CLINICA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, null, null, LimitEnforcement.BLOCK,
                    null, 80, LimitMode.FULL, null, CREADO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("LIMITED requires a quantity");
        }

        @Test
        @DisplayName("declarar FULL con cantidad se rechaza")
        void declarar_FULL_con_cantidad_se_rechaza() {
            assertThatThrownBy(() -> CatalogItemLimit.create(HISTORIA_CLINICA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.FULL, 100, null, LimitEnforcement.BLOCK, null,
                    80, LimitMode.FULL, null, CREADO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("FULL cannot carry a quantity");
        }
    }

    @Nested
    @DisplayName("R-LIMIT-12 · el excedente exige precio y no cabe sobre un acumulativo")
    class Excedente {

        @Test
        @DisplayName("declarar OVERAGE sin precio por unidad se rechaza")
        void declarar_OVERAGE_sin_precio_por_unidad_se_rechaza() {
            assertThatThrownBy(() -> CatalogItemLimit.create(HISTORIA_CLINICA, 4L, MeasureKind.FLOW,
                    LimitMode.LIMITED, 100, ResetPeriod.MONTH, LimitEnforcement.OVERAGE, null, 80,
                    LimitMode.FULL, null, CREADO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive unit price");
        }

        @Test
        @DisplayName("declarar OVERAGE sobre el eje ANIMAL, que es acumulativo, se rechaza")
        void declarar_OVERAGE_sobre_el_eje_ANIMAL_se_rechaza() {
            assertThatThrownBy(() -> CatalogItemLimit.create(HISTORIA_CLINICA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, 100, null, LimitEnforcement.OVERAGE,
                    new BigDecimal("500.00"), 80, LimitMode.FULL, null, CREADO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not fit a CUMULATIVE dimension");
        }

        @Test
        @DisplayName("un precio de excedente sin modo OVERAGE se rechaza")
        void un_precio_de_excedente_sin_modo_OVERAGE_se_rechaza() {
            assertThatThrownBy(() -> CatalogItemLimit.create(HISTORIA_CLINICA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK,
                    new BigDecimal("500.00"), 80, LimitMode.FULL, null, CREADO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only applies to OVERAGE");
        }

        @Test
        @DisplayName("el excedente sí cabe sobre un cupo de flujo con su precio")
        void el_excedente_cabe_sobre_un_cupo_de_flujo_con_su_precio() {
            CatalogItemLimit facturas = CatalogItemLimit.create(HISTORIA_CLINICA, 4L,
                    MeasureKind.FLOW, LimitMode.LIMITED, 100, ResetPeriod.MONTH,
                    LimitEnforcement.OVERAGE, new BigDecimal("500.00"), 80, LimitMode.FULL, null,
                    CREADO);

            assertThat(facturas.getEnforcement()).isEqualTo(LimitEnforcement.OVERAGE);
            assertThat(facturas.getEnforcement().allowsCreationOverLimit()).isTrue();
        }
    }

    @Nested
    @DisplayName("el periodo de reinicio es de los cupos de flujo y solo de ellos")
    class PeriodoDeReinicio {

        @Test
        @DisplayName("un cupo de flujo sin periodo no se reiniciaría nunca: se rechaza")
        void un_cupo_de_flujo_sin_periodo_se_rechaza() {
            assertThatThrownBy(() -> CatalogItemLimit.create(HISTORIA_CLINICA, 4L, MeasureKind.FLOW,
                    LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK, null, 80, LimitMode.FULL,
                    null, CREADO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("never resets");
        }

        @Test
        @DisplayName("un cupo total con periodo se borraría cada mes: se rechaza")
        void un_cupo_total_con_periodo_se_rechaza() {
            assertThatThrownBy(() -> CatalogItemLimit.create(HISTORIA_CLINICA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, 100, ResetPeriod.MONTH,
                    LimitEnforcement.BLOCK, null, 80, LimitMode.FULL, null, CREADO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only applies to a FLOW dimension");
        }
    }

    @Nested
    @DisplayName("R-TRIAL-18 · la prueba va sin techo")
    class TechoDeLaPrueba {

        @Test
        @DisplayName("con 400 mascotas migradas durante la prueba y un cupo gratuito de 100 no hay"
                + " techo que aplicar")
        void ana_migra_400_mascotas_durante_la_prueba_con_un_cupo_gratuito_de_100_y_no_se_bloquea() {
            CatalogItemLimit limite = cienMascotas();

            assertThat(limite.getTrialMode()).isEqualTo(LimitMode.FULL);
            assertThat(limite.effectiveTrialLimit()).isNull();
            assertThat(limite.getLimitQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("el techo de prueba se puede endurecer por artículo sin desplegar nada")
        void el_techo_de_prueba_se_puede_endurecer_por_articulo() {
            CatalogItemLimit limite = CatalogItemLimit.create(HISTORIA_CLINICA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK,
                    null, 80, LimitMode.LIMITED, 500, CREADO);

            assertThat(limite.effectiveTrialLimit()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("el porcentaje de aviso")
    class PorcentajeDeAviso {

        @Test
        @DisplayName("un aviso al 0 % o al 101 % se rechaza")
        void un_aviso_fuera_de_rango_se_rechaza() {
            assertThatThrownBy(() -> CatalogItemLimit.create(HISTORIA_CLINICA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK,
                    null, 0, LimitMode.FULL, null, CREADO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 100");
            assertThatThrownBy(() -> CatalogItemLimit.create(HISTORIA_CLINICA, EJE_ANIMAL,
                    MeasureKind.CUMULATIVE, LimitMode.LIMITED, 100, null, LimitEnforcement.BLOCK,
                    null, 101, LimitMode.FULL, null, CREADO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("cambiar el techo de fábrica")
    class Actualizacion {

        @Test
        @DisplayName("baja el cupo de 100 a 80 sin tocar el eje ni su tipo de medida")
        void baja_el_cupo_sin_tocar_el_eje() {
            CatalogItemLimit limite = cienMascotas();

            limite.update(LimitMode.LIMITED, 80, null, LimitEnforcement.BLOCK, null, 80,
                    LimitMode.FULL, null);

            assertThat(limite.getLimitQuantity()).isEqualTo(80);
            assertThat(limite.getLimitDimensionId()).isEqualTo(EJE_ANIMAL);
            assertThat(limite.getMeasureKind()).isEqualTo(MeasureKind.CUMULATIVE);
        }

        @Test
        @DisplayName("una actualización incoherente se rechaza y no deja la fila a medias")
        void una_actualizacion_incoherente_se_rechaza() {
            CatalogItemLimit limite = cienMascotas();

            assertThatThrownBy(() -> limite.update(LimitMode.LIMITED, null, null,
                    LimitEnforcement.BLOCK, null, 80, LimitMode.FULL, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(limite.getLimitQuantity()).isEqualTo(100);
        }
    }
}
