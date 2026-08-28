package com.vetsoftware.app.companytrialgrant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyTrialGrant — qué probó ya esta empresa, y hasta cuándo")
class CompanyTrialGrantTest {

    private static final Long ANA = 42L;
    private static final Long INVENTARIO = 11L;
    private static final Long CAJA = 12L;
    private static final Long COTIZACION = 7L;
    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate FIN_DE_VENTANA = LocalDate.of(2026, 9, 30);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 9, 1, 8, 0);

    /** Del 1 al 30 de septiembre, abierta. */
    private static TrialWindowRef ventana() {
        return new TrialWindowRef(5L, ANA, INICIO, FIN_DE_VENTANA, true);
    }

    @Nested
    @DisplayName("R-TRIAL-03 · la fecha de fin no se elige, se calcula")
    class DiasHeredados {

        @Test
        @DisplayName("Inventario añadido el día 15 de una ventana del 1 al 30 recibe fin 30 de"
                + " septiembre — 15 días, no 30")
        void inventario_anadido_el_dia_15_de_una_ventana_del_1_al_30_de_septiembre_recibe_fin_30_de_septiembre() {
            CompanyTrialGrant concesion = CompanyTrialGrant.grant(ventana(), INVENTARIO,
                    LocalDate.of(2026, 9, 16), 30, 30, TrialPolicyOutcome.LIMITED, COTIZACION, null,
                    CREADA);

            assertThat(concesion.getTrialEndDate()).isEqualTo(FIN_DE_VENTANA);
            assertThat(concesion.effectiveDays()).isEqualTo(15);
        }

        @Test
        @DisplayName("Caja con 14 días de política concedida el día 0 vence antes que la ventana")
        void caja_con_14_dias_de_politica_concedida_el_dia_0_de_una_ventana_de_30_vence_antes_que_la_ventana() {
            CompanyTrialGrant concesion = CompanyTrialGrant.grant(ventana(), CAJA, INICIO, 14, 14,
                    TrialPolicyOutcome.LIMITED, COTIZACION, null, CREADA);

            assertThat(concesion.getTrialEndDate()).isEqualTo(LocalDate.of(2026, 9, 14));
            assertThat(concesion.effectiveDays()).isEqualTo(14);
        }

        @Test
        @DisplayName("una concesión con fecha de fin posterior al fin de su ventana se rechaza")
        void un_grant_con_trial_end_date_posterior_al_end_date_de_su_ventana_muere() {
            assertThatThrownBy(() -> new CompanyTrialGrant(1L, ANA, INVENTARIO, 5L, FIN_DE_VENTANA,
                    LocalDate.of(2026, 9, 16), 30, LocalDate.of(2026, 10, 15), 30,
                    TrialPolicyOutcome.LIMITED, COTIZACION, null, null, null, CREADA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2026-09-30");
        }

        @Test
        @DisplayName("conceder el día 35 de una ventana de 30 no entra en prueba")
        void conceder_pasado_el_fin_de_ventana_no_entra_en_prueba() {
            assertThatThrownBy(
                    () -> CompanyTrialGrant.grant(ventana(), INVENTARIO, LocalDate.of(2026, 10, 5),
                            30, 30, TrialPolicyOutcome.LIMITED, COTIZACION, null, CREADA))
                    .isInstanceOf(TrialWindowNotOpenException.class)
                    .hasMessageContaining("added as paid");
        }

        @Test
        @DisplayName("conceder sobre una ventana ya cerrada no entra en prueba")
        void conceder_sobre_una_ventana_cerrada_no_entra_en_prueba() {
            TrialWindowRef cerrada = new TrialWindowRef(5L, ANA, INICIO, FIN_DE_VENTANA, false);

            assertThatThrownBy(
                    () -> CompanyTrialGrant.grant(cerrada, INVENTARIO, LocalDate.of(2026, 9, 10),
                            30, 30, TrialPolicyOutcome.LIMITED, COTIZACION, null, CREADA))
                    .isInstanceOf(TrialWindowNotOpenException.class);
        }
    }

    @Nested
    @DisplayName("R-TRIAL-19 · nadie prueba más de lo que el catálogo permite")
    class TopeDeDias {

        @Test
        @DisplayName("una oferta de 3.650 días de prueba sobre una política de 30 se rechaza")
        void una_oferta_de_3650_dias_de_prueba_muere() {
            assertThatThrownBy(() -> CompanyTrialGrant.grant(ventana(), INVENTARIO, INICIO, 3650,
                    30, TrialPolicyOutcome.LIMITED, COTIZACION, null, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot exceed the frozen policy");
        }

        @Test
        @DisplayName("una campaña sí puede conceder menos días que la política")
        void una_campana_puede_conceder_menos_dias_que_la_politica() {
            CompanyTrialGrant concesion = CompanyTrialGrant.grant(ventana(), CAJA, INICIO, 7, 14,
                    TrialPolicyOutcome.LIMITED, COTIZACION, null, CREADA);

            assertThat(concesion.getTrialEndDate()).isEqualTo(LocalDate.of(2026, 9, 7));
        }
    }

    /**
     * El caso «no existe ninguna operación que borre o desactive» vivía aquí y
     * reflexionaba sobre el agregado y nada más: un método de repositorio, una
     * consulta nativa o un puerto de entrada nuevo pasaban por debajo sin tocarlo.
     * Se movió, ampliado a toda la superficie de escritura del slice, a
     * {@code companytrialgrant.TrialGrantWriteSurfaceTest}.
     */
    @Nested
    @DisplayName("R-TRIAL-22 y R-TRIAL-30 · no se desconcede, se resuelve")
    class Desenlace {

        @Test
        @DisplayName("quitar un módulo en prueba antes de vencer lo marca ABANDONED y no deja la"
                + " concesión viva")
        void quitar_un_modulo_en_prueba_antes_de_vencer_marca_ABANDONED_y_no_deja_la_concesion_viva() {
            CompanyTrialGrant concesion = CompanyTrialGrant.grant(ventana(), INVENTARIO, INICIO, 30,
                    30, TrialPolicyOutcome.LIMITED, COTIZACION, null, CREADA);

            CompanyTrialGrant resuelta = concesion.consume(LocalDateTime.of(2026, 9, 20, 10, 0),
                    TrialOutcome.ABANDONED);

            assertThat(resuelta.getOutcome()).isEqualTo(TrialOutcome.ABANDONED);
            assertThat(resuelta.isLive()).isFalse();
            assertThat(resuelta.getTrialEndDate()).isEqualTo(concesion.getTrialEndDate());
        }

        @Test
        @DisplayName("resolver dos veces la misma prueba se rechaza: movería la tasa de conversión")
        void resolver_dos_veces_la_misma_prueba_se_rechaza() {
            CompanyTrialGrant resuelta = CompanyTrialGrant
                    .grant(ventana(), INVENTARIO, INICIO, 30, 30, TrialPolicyOutcome.LIMITED,
                            COTIZACION, null, CREADA)
                    .consume(LocalDateTime.of(2026, 9, 20, 10, 0), TrialOutcome.ABANDONED);

            assertThatThrownBy(() -> resuelta.consume(LocalDateTime.of(2026, 9, 21, 10, 0),
                    TrialOutcome.CONVERTED)).isInstanceOf(TrialAlreadyConsumedException.class);
        }

        @Test
        @DisplayName("un desenlace sin fecha de resolución no es un resultado")
        void un_desenlace_sin_fecha_de_resolucion_se_rechaza() {
            assertThatThrownBy(() -> new CompanyTrialGrant(1L, ANA, INVENTARIO, 5L, FIN_DE_VENTANA,
                    INICIO, 30, FIN_DE_VENTANA, 30, TrialPolicyOutcome.LIMITED, COTIZACION, null,
                    null, TrialOutcome.CONVERTED, CREADA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a result");
        }
    }

    @Nested
    @DisplayName("R-TRIAL-26 · el último día es inclusivo")
    class UltimoDiaInclusivo {

        @Test
        @DisplayName("una prueba que termina el 30 de septiembre sigue viva ese mismo día")
        void una_prueba_que_termina_el_30_de_septiembre_sigue_viva_el_30_de_septiembre() {
            CompanyTrialGrant concesion = CompanyTrialGrant.grant(ventana(), INVENTARIO, INICIO, 30,
                    30, TrialPolicyOutcome.LIMITED, COTIZACION, null, CREADA);

            assertThat(concesion.isActiveOn(LocalDate.of(2026, 9, 30))).isTrue();
            assertThat(concesion.isActiveOn(LocalDate.of(2026, 10, 1))).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una concesión sin papel que la conceda se rechaza")
        void una_concesion_sin_papel_se_rechaza() {
            assertThatThrownBy(() -> CompanyTrialGrant.grant(ventana(), INVENTARIO, INICIO, 30, 30,
                    TrialPolicyOutcome.LIMITED, null, null, CREADA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one of source quote or granting" + " amendment");
        }

        @Test
        @DisplayName("una concesión con cotización y otrosí a la vez se rechaza")
        void una_concesion_con_dos_papeles_se_rechaza() {
            assertThatThrownBy(() -> CompanyTrialGrant.grant(ventana(), INVENTARIO, INICIO, 30, 30,
                    TrialPolicyOutcome.LIMITED, COTIZACION, 99L, CREADA))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("una ventana de otra clínica no puede sostener la prueba de esta")
        void una_concesion_no_puede_colgar_de_la_ventana_de_otra_clinica() {
            TrialWindowRef ajena = new TrialWindowRef(9L, 99L, INICIO, FIN_DE_VENTANA, true);

            CompanyTrialGrant concesion = CompanyTrialGrant.grant(ajena, INVENTARIO, INICIO, 30, 30,
                    TrialPolicyOutcome.LIMITED, COTIZACION, null, CREADA);

            assertThat(concesion.getCompanyId()).isEqualTo(99L);
        }
    }
}
