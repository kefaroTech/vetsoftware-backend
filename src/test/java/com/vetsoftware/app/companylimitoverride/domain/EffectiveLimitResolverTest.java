package com.vetsoftware.app.companylimitoverride.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EffectiveLimitResolver — la precedencia del techo, en un solo sitio")
class EffectiveLimitResolverTest {

    private static final Long EXCEPCION = 77L;

    @Nested
    @DisplayName("R-LIMIT-06 · entre orígenes distintos manda el de más arriba")
    class Precedencia {

        @Test
        @DisplayName("con excepción de 300, techo contratado de 200 y fábrica de 100 lee 300, con"
                + " origen COMPANY_OVERRIDE")
        void ana_con_excepcion_de_300_techo_contratado_de_200_y_fabrica_de_100_lee_300_con_origen_COMPANY_OVERRIDE() {
            EffectiveLimit techo = EffectiveLimitResolver.resolve(300, EXCEPCION, List.of(200),
                    List.of(100), true);

            assertThat(techo.limitQuantity()).isEqualTo(300);
            assertThat(techo.source()).isEqualTo(LimitSource.COMPANY_OVERRIDE);
            assertThat(techo.overrideId()).isEqualTo(EXCEPCION);
        }

        @Test
        @DisplayName("sin excepción, lo contratado manda sobre el escalón de fábrica")
        void sin_excepcion_lo_contratado_manda_sobre_fabrica() {
            EffectiveLimit techo = EffectiveLimitResolver.resolve(null, null, List.of(200),
                    List.of(100), true);

            assertThat(techo.limitQuantity()).isEqualTo(200);
            assertThat(techo.source()).isEqualTo(LimitSource.SUBSCRIPTION);
            assertThat(techo.overrideId()).isNull();
        }

        @Test
        @DisplayName("un techo de origen COMPANY_OVERRIDE sin nombrar su excepción se rechaza")
        void un_techo_negociado_sin_nombrar_su_excepcion_se_rechaza() {
            assertThatThrownBy(
                    () -> EffectiveLimitResolver.resolve(300, null, List.of(), List.of(), true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must name the override");
        }
    }

    @Nested
    @DisplayName("R-LIMIT-07 · dentro de un origen se toma el máximo, nunca la suma")
    class SueloNoSumando {

        @Test
        @DisplayName("tres usuarios comprados más un módulo gratuito con cupo 1 dan 3, no 4")
        void tres_usuarios_comprados_mas_un_modulo_gratuito_con_cupo_1_dan_3_no_4() {
            EffectiveLimit techo = EffectiveLimitResolver.resolve(null, null, List.of(3),
                    List.of(1), true);

            assertThat(techo.limitQuantity()).isEqualTo(3);
            assertThat(techo.source()).isEqualTo(LimitSource.SUBSCRIPTION);
        }

        @Test
        @DisplayName("activarse cuatro módulos gratuitos no infla el mismo cupo: sigue siendo 1")
        void activarse_cuatro_modulos_gratuitos_no_infla_el_mismo_cupo() {
            EffectiveLimit techo = EffectiveLimitResolver.resolve(null, null, List.of(),
                    List.of(1, 1, 1, 1), true);

            assertThat(techo.limitQuantity()).isEqualTo(1);
            assertThat(techo.source()).isEqualTo(LimitSource.CATALOG_DEFAULT);
        }

        @Test
        @DisplayName("entre varias líneas contratadas gana la mayor, no la suma")
        void entre_varias_lineas_contratadas_gana_la_mayor() {
            EffectiveLimit techo = EffectiveLimitResolver.resolve(null, null, List.of(3, 10, 5),
                    List.of(), true);

            assertThat(techo.limitQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("una línea sin techo gana a cualquier número dentro del mismo origen")
        void una_linea_sin_techo_gana_a_cualquier_numero() {
            EffectiveLimit techo = EffectiveLimitResolver.resolve(null, null,
                    java.util.Arrays.asList(3, null, 5), List.of(), true);

            assertThat(techo.isUnlimited()).isTrue();
            assertThat(techo.source()).isEqualTo(LimitSource.SUBSCRIPTION);
        }
    }

    @Nested
    @DisplayName("R-LIMIT-08 · sin fila, cero — salvo que el eje sea posterior a la firma")
    class SinFila {

        @Test
        @DisplayName("un eje existente sin fila para una empresa da techo cero, no ilimitado")
        void un_eje_existente_sin_fila_para_una_empresa_da_techo_cero_no_ilimitado() {
            EffectiveLimit techo = EffectiveLimitResolver.resolve(null, null, List.of(), List.of(),
                    true);

            assertThat(techo.limitQuantity()).isZero();
            assertThat(techo.isUnlimited()).isFalse();
            assertThat(techo.source()).isEqualTo(LimitSource.NONE);
        }

        @Test
        @DisplayName("añadir un eje en abril no deja bloqueadas las agendas de contratos firmados"
                + " en enero")
        void anadir_el_eje_APPOINTMENT_en_abril_no_deja_bloqueadas_las_agendas_firmadas_en_enero() {
            EffectiveLimit techo = EffectiveLimitResolver.resolve(null, null, List.of(), List.of(),
                    false);

            assertThat(techo.isUnlimited()).isTrue();
            assertThat(techo.source()).isEqualTo(LimitSource.NONE);
        }
    }

    @Nested
    @DisplayName("EffectiveLimit — si se pasa del techo")
    class SePasa {

        @Test
        @DisplayName("crear la mascota 101 sobre un cupo de 100 se pasa; la 100 no")
        void crear_la_mascota_101_sobre_un_cupo_de_100_se_pasa() {
            EffectiveLimit techo = new EffectiveLimit(100, LimitSource.CATALOG_DEFAULT, null);

            assertThat(techo.wouldExceed(100, 1)).isTrue();
            assertThat(techo.wouldExceed(99, 1)).isFalse();
        }

        @Test
        @DisplayName("sin techo nunca se pasa")
        void sin_techo_nunca_se_pasa() {
            EffectiveLimit techo = new EffectiveLimit(null, LimitSource.SUBSCRIPTION, null);

            assertThat(techo.wouldExceed(Integer.MAX_VALUE, 1)).isFalse();
        }

        @Test
        @DisplayName("R-LIMIT-38 · un consumo por encima del techo es un estado válido y no revienta")
        void bajar_el_techo_a_100_con_400_mascotas_no_hace_fallar_el_calculo() {
            EffectiveLimit techo = new EffectiveLimit(100, LimitSource.CATALOG_DEFAULT, null);

            assertThat(techo.wouldExceed(400, 0)).isTrue();
            assertThat(techo.limitQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("un techo negativo se rechaza")
        void un_techo_negativo_se_rechaza() {
            assertThatThrownBy(() -> new EffectiveLimit(-1, LimitSource.CATALOG_DEFAULT, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("un techo que no es negociado no puede nombrar una excepción")
        void un_techo_no_negociado_no_nombra_excepcion() {
            assertThatThrownBy(() -> new EffectiveLimit(100, LimitSource.SUBSCRIPTION, 5L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
