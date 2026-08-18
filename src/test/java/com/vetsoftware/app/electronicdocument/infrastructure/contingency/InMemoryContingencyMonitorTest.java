package com.vetsoftware.app.electronicdocument.infrastructure.contingency;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryContingencyMonitor — detecta la caida sostenida del proveedor por empresa")
class InMemoryContingencyMonitorTest {

    @Nested
    @DisplayName("companyId null — no-op")
    class CompanyIdNull {

        @Test
        @DisplayName("recordOutcome con companyId null no falla y no activa nada")
        void record_outcome_con_company_id_null_no_falla() {
            InMemoryContingencyMonitor monitor = new InMemoryContingencyMonitor(3);

            monitor.recordOutcome(null, false);

            assertThat(monitor.isActive(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("umbral de fallos consecutivos")
    class UmbralDeFallos {

        @Test
        @DisplayName("una empresa nueva nunca esta en contingencia")
        void empresa_nueva_nunca_esta_en_contingencia() {
            InMemoryContingencyMonitor monitor = new InMemoryContingencyMonitor(3);

            assertThat(monitor.isActive(9L)).isFalse();
        }

        @Test
        @DisplayName("fallos por debajo del umbral no activan la contingencia")
        void fallos_por_debajo_del_umbral_no_activan() {
            InMemoryContingencyMonitor monitor = new InMemoryContingencyMonitor(3);

            monitor.recordOutcome(9L, false);
            monitor.recordOutcome(9L, false);

            assertThat(monitor.isActive(9L)).isFalse();
        }

        @Test
        @DisplayName("al alcanzar el umbral de fallos consecutivos se activa la contingencia")
        void al_alcanzar_el_umbral_se_activa() {
            InMemoryContingencyMonitor monitor = new InMemoryContingencyMonitor(3);

            monitor.recordOutcome(9L, false);
            monitor.recordOutcome(9L, false);
            monitor.recordOutcome(9L, false);

            assertThat(monitor.isActive(9L)).isTrue();
        }

        @Test
        @DisplayName("un exito intermedio reinicia el contador de fallos consecutivos")
        void un_exito_intermedio_reinicia_el_contador() {
            InMemoryContingencyMonitor monitor = new InMemoryContingencyMonitor(3);

            monitor.recordOutcome(9L, false);
            monitor.recordOutcome(9L, false);
            monitor.recordOutcome(9L, true);
            monitor.recordOutcome(9L, false);
            monitor.recordOutcome(9L, false);

            assertThat(monitor.isActive(9L)).isFalse();
        }

        @Test
        @DisplayName("un umbral configurado en cero se trata como uno (al menos un fallo activa)")
        void umbral_cero_se_trata_como_uno() {
            InMemoryContingencyMonitor monitor = new InMemoryContingencyMonitor(0);

            monitor.recordOutcome(9L, false);

            assertThat(monitor.isActive(9L)).isTrue();
        }
    }

    @Nested
    @DisplayName("recuperacion — un exito desactiva la contingencia")
    class Recuperacion {

        @Test
        @DisplayName("tras activarse, el primer resultado sano desactiva la contingencia")
        void primer_resultado_sano_desactiva() {
            InMemoryContingencyMonitor monitor = new InMemoryContingencyMonitor(2);
            monitor.recordOutcome(9L, false);
            monitor.recordOutcome(9L, false);
            assertThat(monitor.isActive(9L)).isTrue();

            monitor.recordOutcome(9L, true);

            assertThat(monitor.isActive(9L)).isFalse();
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("la contingencia de una empresa no afecta a otra")
        void contingencia_de_una_empresa_no_afecta_a_otra() {
            InMemoryContingencyMonitor monitor = new InMemoryContingencyMonitor(2);

            monitor.recordOutcome(9L, false);
            monitor.recordOutcome(9L, false);

            assertThat(monitor.isActive(9L)).isTrue();
            assertThat(monitor.isActive(77L)).isFalse();
        }

        @Test
        @DisplayName("isActive con companyId null siempre es false")
        void is_active_con_company_id_null_es_false() {
            InMemoryContingencyMonitor monitor = new InMemoryContingencyMonitor(2);

            assertThat(monitor.isActive(null)).isFalse();
        }
    }
}
