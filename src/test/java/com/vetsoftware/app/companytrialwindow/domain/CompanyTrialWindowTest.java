package com.vetsoftware.app.companytrialwindow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyTrialWindow — el reloj de la empresa")
class CompanyTrialWindowTest {

    private static final Long ANA = 42L;
    private static final Long COTIZACION = 7L;
    private static final LocalDate UNO_DE_SEPTIEMBRE = LocalDate.of(2026, 9, 1);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 9, 1, 8, 0);

    private static CompanyTrialWindow ventanaDe30Dias() {
        return CompanyTrialWindow.open(ANA, UNO_DE_SEPTIEMBRE, 30, COTIZACION, CREADA);
    }

    @Nested
    @DisplayName("R-TRIAL-02 · el fin es inclusivo")
    class FinInclusivo {

        @Test
        @DisplayName("una ventana de 30 días abierta el 1 de septiembre termina el 30, no el 1 de"
                + " octubre")
        void una_ventana_de_30_dias_abierta_el_1_de_septiembre_termina_el_30_de_septiembre_no_el_1_de_octubre() {
            assertThat(ventanaDe30Dias().getEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        }

        @Test
        @DisplayName("el código que suma los días sin restar uno regala un día por cliente y ahora"
                + " muere al construir la ventana")
        void el_codigo_que_suma_los_dias_sin_restar_uno_regala_un_dia_por_cliente_y_ahora_muere() {
            assertThatThrownBy(() -> new CompanyTrialWindow(1L, ANA, UNO_DE_SEPTIEMBRE,
                    LocalDate.of(2026, 10, 1), 30, COTIZACION, null, CREADA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2026-09-30");
        }

        @Test
        @DisplayName("catorce días desde el día cero vencen el catorce, no el quince")
        void catorce_dias_desde_el_dia_cero_vencen_el_catorce() {
            assertThat(CompanyTrialWindow.lastDayOf(UNO_DE_SEPTIEMBRE, 14))
                    .isEqualTo(LocalDate.of(2026, 9, 14));
        }

        @Test
        @DisplayName("una ventana de cero días no es una ventana")
        void una_ventana_de_cero_dias_se_rechaza() {
            assertThatThrownBy(
                    () -> CompanyTrialWindow.open(ANA, UNO_DE_SEPTIEMBRE, 0, COTIZACION, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than zero");
        }
    }

    /**
     * El caso «no existe ninguna operación que amplíe los días» vivía aquí y
     * reflexionaba sobre el agregado y nada más: un método de repositorio, una
     * consulta nativa o un puerto de entrada nuevo pasaban por debajo sin tocarlo.
     * Se movió, ampliado a toda la superficie de escritura del slice, a
     * {@code companytrialwindow.TrialWindowWriteSurfaceTest}.
     */
    @Nested
    @DisplayName("R-TRIAL-09 y R-TRIAL-10 · la ventana no se estira y el cierre manda")
    class NoSeEstira {

        @Test
        @DisplayName("añadir un módulo el día 35 de una ventana de 30 no cabe en la ventana")
        void anadir_un_modulo_el_dia_35_de_una_ventana_de_30_entra_pagando_no_en_prueba() {
            assertThat(ventanaDe30Dias().admitsGrantOn(LocalDate.of(2026, 10, 5))).isFalse();
        }

        @Test
        @DisplayName("una ventana cerrada no admite conceder nada, aunque el día caiga dentro")
        void una_ventana_cerrada_no_admite_conceder_aunque_el_dia_caiga_dentro() {
            CompanyTrialWindow cerrada = ventanaDe30Dias()
                    .close(LocalDateTime.of(2026, 9, 10, 12, 0));

            assertThat(cerrada.admitsGrantOn(LocalDate.of(2026, 9, 15))).isFalse();
            assertThat(cerrada.isOpen()).isFalse();
        }

        @Test
        @DisplayName("cerrar dos veces la misma ventana se rechaza: movería la fecha auditada")
        void cerrar_dos_veces_la_misma_ventana_se_rechaza() {
            CompanyTrialWindow cerrada = ventanaDe30Dias()
                    .close(LocalDateTime.of(2026, 9, 10, 12, 0));

            assertThatThrownBy(() -> cerrada.close(LocalDateTime.of(2026, 9, 11, 12, 0)))
                    .isInstanceOf(TrialWindowAlreadyClosedException.class)
                    .hasMessageContaining("already closed");
        }
    }

    @Nested
    @DisplayName("los días que quedan, que es lo que hereda un módulo añadido a mitad")
    class DiasQueQuedan {

        @Test
        @DisplayName("el día 15 de una ventana de 30 quedan 16 días contando el propio y el último")
        void el_dia_15_de_una_ventana_de_30_quedan_16_dias() {
            assertThat(ventanaDe30Dias().remainingDaysFrom(LocalDate.of(2026, 9, 15)))
                    .isEqualTo(16);
        }

        @Test
        @DisplayName("el último día queda uno, no cero: el fin es inclusivo")
        void el_ultimo_dia_queda_uno() {
            assertThat(ventanaDe30Dias().remainingDaysFrom(LocalDate.of(2026, 9, 30))).isEqualTo(1);
        }

        @Test
        @DisplayName("pasado el fin no queda ninguno")
        void pasado_el_fin_no_queda_ninguno() {
            assertThat(ventanaDe30Dias().remainingDaysFrom(LocalDate.of(2026, 10, 1))).isZero();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una ventana sin empresa se rechaza")
        void una_ventana_sin_empresa_se_rechaza() {
            assertThatThrownBy(
                    () -> CompanyTrialWindow.open(null, UNO_DE_SEPTIEMBRE, 30, COTIZACION, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id");
        }

        @Test
        @DisplayName("una ventana sin cotización de origen se rechaza: no hay dos puertas de"
                + " entrada")
        void una_ventana_sin_cotizacion_se_rechaza() {
            assertThatThrownBy(
                    () -> CompanyTrialWindow.open(ANA, UNO_DE_SEPTIEMBRE, 30, null, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("source quote id");
        }

        @Test
        @DisplayName("cerrarla antes de empezar se rechaza")
        void cerrarla_antes_de_empezar_se_rechaza() {
            assertThatThrownBy(() -> new CompanyTrialWindow(1L, ANA, UNO_DE_SEPTIEMBRE,
                    LocalDate.of(2026, 9, 30), 30, COTIZACION, LocalDateTime.of(2026, 8, 20, 10, 0),
                    CREADA, 0L)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot precede the start date");
        }
    }
}
