package com.vetsoftware.app.companyactivitymonth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("CompanyActivityMonth")
class CompanyActivityMonthTest {

    private static final Long COMPANY_ID = 9L;
    private static final ActivityPeriodKey MARZO_2026 = new ActivityPeriodKey("2026-03");

    /** 2026 no es bisiesto: 28 dias. */
    private static final ActivityPeriodKey FEBRERO_2026 = new ActivityPeriodKey("2026-02");

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 1, 0, 5);

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            CompanyActivityMonth mes = new CompanyActivityMonth(1L, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 20, 5, 340, new BigDecimal("199990.00"), CREADO, 0L);

            assertThat(mes.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(mes.getPeriodKey()).isEqualTo(MARZO_2026);
            assertThat(mes.getCommercialState()).isEqualTo(CommercialState.PAID);
            assertThat(mes.getActiveDays()).isEqualTo(20);
            assertThat(mes.getActiveUsers()).isEqualTo(5);
            assertThat(mes.getRecordsCreated()).isEqualTo(340);
            assertThat(mes.getMrrSnapshot()).isEqualByComparingTo("199990.00");
            assertThat(mes.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("companyId es obligatorio")
        void company_id_es_obligatorio() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, null, MARZO_2026,
                    CommercialState.PAID, 1, 1, 1, BigDecimal.ZERO, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("periodKey es obligatoria")
        void period_key_es_obligatoria() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, null,
                    CommercialState.PAID, 1, 1, 1, BigDecimal.ZERO, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("periodKey is required");
        }

        @Test
        @DisplayName("commercialState es obligatorio")
        void commercial_state_es_obligatorio() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026, null, 1,
                    1, 1, BigDecimal.ZERO, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("commercialState is required");
        }

        @Test
        @DisplayName("createdDate es obligatoria")
        void created_date_es_obligatoria() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 1, 1, 1, BigDecimal.ZERO, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("createdDate is required");
        }
    }

    @Nested
    @DisplayName("dias activos: chk_cam_active_days mas el techo real del mes")
    class DiasActivos {

        @Test
        @DisplayName("un valor negativo se rechaza")
        void un_valor_negativo_se_rechaza() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, -1, 1, 1, BigDecimal.ZERO, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("activeDays must be between 0 and 31");
        }

        @Test
        @DisplayName("un valor mayor que 31 se rechaza aunque el mes tuviera mas dias")
        void un_valor_mayor_que_31_se_rechaza() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 32, 1, 1, BigDecimal.ZERO, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("activeDays must be between 0 and 31");
        }

        @Test
        @DisplayName("un valor que excede los dias reales de febrero se rechaza")
        void un_valor_que_excede_los_dias_reales_de_febrero_se_rechaza() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, FEBRERO_2026,
                    CommercialState.PAID, 29, 1, 1, BigDecimal.ZERO, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("activeDays cannot exceed the 28 days of 2026-02");
        }

        @Test
        @DisplayName("el techo real del mes se acepta")
        void el_techo_real_del_mes_se_acepta() {
            CompanyActivityMonth mes = new CompanyActivityMonth(null, COMPANY_ID, FEBRERO_2026,
                    CommercialState.PAID, 28, 1, 1, BigDecimal.ZERO, CREADO, null);

            assertThat(mes.getActiveDays()).isEqualTo(28);
        }

        @Test
        @DisplayName("cero dias activos es valido")
        void cero_dias_activos_es_valido() {
            CompanyActivityMonth mes = new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.FREE, 0, 0, 0, BigDecimal.ZERO, CREADO, null);

            assertThat(mes.getActiveDays()).isZero();
        }
    }

    @Nested
    @DisplayName("contadores: activeUsers y recordsCreated no pueden ser negativos")
    class Contadores {

        @Test
        @DisplayName("activeUsers negativo se rechaza")
        void active_users_negativo_se_rechaza() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 1, -1, 1, BigDecimal.ZERO, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("activeUsers must not be negative");
        }

        @Test
        @DisplayName("recordsCreated negativo se rechaza")
        void records_created_negativo_se_rechaza() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 1, 1, -1, BigDecimal.ZERO, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("recordsCreated must not be negative");
        }
    }

    @Nested
    @DisplayName("chk_cam_mrr: el MRR nunca es negativo, pero cero si es legitimo")
    class Mrr {

        @Test
        @DisplayName("mrrSnapshot es obligatorio")
        void mrr_snapshot_es_obligatorio() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 1, 1, 1, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mrrSnapshot is required");
        }

        @Test
        @DisplayName("un MRR negativo se rechaza")
        void un_mrr_negativo_se_rechaza() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 1, 1, 1, new BigDecimal("-0.01"), CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mrrSnapshot must not be negative");
        }

        @Test
        @DisplayName("un MRR cero es legitimo: no es lo mismo que negativo")
        void un_mrr_cero_es_legitimo() {
            CompanyActivityMonth mes = new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.FREE, 0, 0, 0, BigDecimal.ZERO, CREADO, null);

            assertThat(mes.getMrrSnapshot()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("un tercer decimal se rechaza porque MySQL lo redondearia en silencio")
        void un_tercer_decimal_se_rechaza() {
            assertThatThrownBy(() -> new CompanyActivityMonth(null, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 1, 1, 1, new BigDecimal("10.123"), CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mrrSnapshot must have 2 decimals or fewer");
        }
    }

    @Nested
    @DisplayName("record")
    class Record {

        @Test
        @DisplayName("nace sin id ni version")
        void nace_sin_id_ni_version() {
            CompanyActivityMonth mes = CompanyActivityMonth.record(COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 20, 5, 340, new BigDecimal("199990.00"), CREADO);

            assertThat(mes.getId()).isNull();
            assertThat(mes.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("recalculate")
    class Recalculate {

        @Test
        @DisplayName("cambia los cinco numeros y conserva identidad, periodo y version")
        void cambia_los_numeros_y_conserva_identidad_periodo_y_version() {
            CompanyActivityMonth original = new CompanyActivityMonth(1L, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 20, 5, 340, new BigDecimal("199990.00"), CREADO, 3L);

            CompanyActivityMonth recalculado = original.recalculate(CommercialState.CHURNED, 2, 1,
                    10, BigDecimal.ZERO);

            assertThat(recalculado.getId()).isEqualTo(original.getId());
            assertThat(recalculado.getCompanyId()).isEqualTo(original.getCompanyId());
            assertThat(recalculado.getPeriodKey()).isEqualTo(original.getPeriodKey());
            assertThat(recalculado.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(recalculado.getVersion()).isEqualTo(original.getVersion());
            assertThat(recalculado.getCommercialState()).isEqualTo(CommercialState.CHURNED);
            assertThat(recalculado.getActiveDays()).isEqualTo(2);
            assertThat(recalculado.getActiveUsers()).isEqualTo(1);
            assertThat(recalculado.getRecordsCreated()).isEqualTo(10);
            assertThat(recalculado.getMrrSnapshot()).isEqualByComparingTo(BigDecimal.ZERO);

            // inmutabilidad: la fila original no cambia
            assertThat(original.getCommercialState()).isEqualTo(CommercialState.PAID);
            assertThat(original.getActiveDays()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("isDormant")
    class IsDormant {

        @Test
        @DisplayName("por debajo del umbral esta dormida")
        void por_debajo_del_umbral_esta_dormida() {
            CompanyActivityMonth mes = new CompanyActivityMonth(1L, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 20, 5, 340, BigDecimal.TEN, CREADO, 0L);

            assertThat(mes.isDormant(25)).isTrue();
        }

        @Test
        @DisplayName("exactamente el umbral cuenta como dormida")
        void exactamente_el_umbral_cuenta_como_dormida() {
            CompanyActivityMonth mes = new CompanyActivityMonth(1L, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 20, 5, 340, BigDecimal.TEN, CREADO, 0L);

            assertThat(mes.isDormant(20)).isTrue();
        }

        @Test
        @DisplayName("por encima del umbral no esta dormida")
        void por_encima_del_umbral_no_esta_dormida() {
            CompanyActivityMonth mes = new CompanyActivityMonth(1L, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 20, 5, 340, BigDecimal.TEN, CREADO, 0L);

            assertThat(mes.isDormant(10)).isFalse();
        }
    }

    @Nested
    @DisplayName("isPaid")
    class IsPaid {

        @Test
        @DisplayName("PAID cuenta como pagado")
        void paid_cuenta_como_pagado() {
            CompanyActivityMonth mes = new CompanyActivityMonth(1L, COMPANY_ID, MARZO_2026,
                    CommercialState.PAID, 20, 5, 340, BigDecimal.TEN, CREADO, 0L);

            assertThat(mes.isPaid()).isTrue();
        }

        @ParameterizedTest(name = "{0} no cuenta como pagado")
        @EnumSource(value = CommercialState.class, names = "PAID", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("gratis, en prueba o dado de baja no es pagar")
        void los_estados_distintos_de_paid_no_cuentan_como_pagado(CommercialState estado) {
            CompanyActivityMonth mes = new CompanyActivityMonth(1L, COMPANY_ID, MARZO_2026, estado,
                    0, 0, 0, BigDecimal.ZERO, CREADO, 0L);

            assertThat(mes.isPaid()).isFalse();
        }
    }
}
