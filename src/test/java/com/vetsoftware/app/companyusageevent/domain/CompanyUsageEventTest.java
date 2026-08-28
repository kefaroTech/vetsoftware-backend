package com.vetsoftware.app.companyusageevent.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyUsageEvent")
class CompanyUsageEventTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long DIMENSION_ID = 5L;
    private static final Long ANIMAL_ID = 100L;
    private static final Long CHARGE_ID = 300L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 3, 10, 9, 14);
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 10, 23, 0);
    private static final UsagePeriodKey PERIOD_KEY = UsagePeriodKey.of("2026-03");

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            CompanyUsageEvent evento = new CompanyUsageEvent(1L, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, null, CREADO, 0L);

            assertThat(evento.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(evento.getLimitDimensionId()).isEqualTo(DIMENSION_ID);
            assertThat(evento.getBranch()).isEqualTo(UsageBranch.ANIMAL);
            assertThat(evento.getUsageReferenceId()).isEqualTo(ANIMAL_ID);
            assertThat(evento.getOccurredAt()).isEqualTo(OCCURRED_AT);
            assertThat(evento.getPeriodKey()).isEqualTo(PERIOD_KEY);
            assertThat(evento.isBillable()).isTrue();
            assertThat(evento.getChargeId()).isNull();
            assertThat(evento.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("companyId es obligatorio")
        void company_id_es_obligatorio() {
            assertThatThrownBy(
                    () -> new CompanyUsageEvent(null, null, DIMENSION_ID, UsageBranch.ANIMAL,
                            ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("limitDimensionId es obligatorio")
        void limit_dimension_id_es_obligatorio() {
            assertThatThrownBy(
                    () -> new CompanyUsageEvent(null, COMPANY_ID, null, UsageBranch.ANIMAL,
                            ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("limitDimensionId is required");
        }

        @Test
        @DisplayName("branch es obligatoria")
        void branch_es_obligatoria() {
            assertThatThrownBy(() -> new CompanyUsageEvent(null, COMPANY_ID, DIMENSION_ID, null,
                    ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch is required");
        }

        @Test
        @DisplayName("usageReferenceId es obligatoria")
        void usage_reference_id_es_obligatoria() {
            assertThatThrownBy(() -> new CompanyUsageEvent(null, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, null, OCCURRED_AT, PERIOD_KEY, true, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("usageReferenceId is required");
        }

        @Test
        @DisplayName("occurredAt es obligatorio")
        void occurred_at_es_obligatorio() {
            assertThatThrownBy(() -> new CompanyUsageEvent(null, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, null, PERIOD_KEY, true, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("occurredAt is required");
        }

        @Test
        @DisplayName("periodKey es obligatoria")
        void period_key_es_obligatoria() {
            assertThatThrownBy(() -> new CompanyUsageEvent(null, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, OCCURRED_AT, null, true, null, CREADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("periodKey is required");
        }

        @Test
        @DisplayName("createdDate es obligatoria")
        void created_date_es_obligatoria() {
            assertThatThrownBy(() -> new CompanyUsageEvent(null, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("createdDate is required");
        }
    }

    @Nested
    @DisplayName("chk_cue_billable: un hecho con cargo tiene que ser facturable")
    class Facturabilidad {

        @Test
        @DisplayName("un cargo sobre un hecho no facturable se rechaza")
        void un_cargo_sobre_un_hecho_no_facturable_se_rechaza() {
            assertThatThrownBy(
                    () -> new CompanyUsageEvent(1L, COMPANY_ID, DIMENSION_ID, UsageBranch.ANIMAL,
                            ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, false, CHARGE_ID, CREADO, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be billable");
        }

        @Test
        @DisplayName("un cargo sobre un hecho facturable es valido")
        void un_cargo_sobre_un_hecho_facturable_es_valido() {
            CompanyUsageEvent evento = new CompanyUsageEvent(1L, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, CHARGE_ID, CREADO,
                    0L);

            assertThat(evento.isCharged()).isTrue();
        }

        @Test
        @DisplayName("un hecho no facturable sin cargo es valido")
        void un_hecho_no_facturable_sin_cargo_es_valido() {
            CompanyUsageEvent evento = new CompanyUsageEvent(1L, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, false, null, CREADO,
                    0L);

            assertThat(evento.isCharged()).isFalse();
        }
    }

    @Nested
    @DisplayName("record")
    class Record {

        @Test
        @DisplayName("nace sin cargo y sin id")
        void nace_sin_cargo_y_sin_id() {
            CompanyUsageEvent evento = CompanyUsageEvent.record(COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, CREADO);

            assertThat(evento.getId()).isNull();
            assertThat(evento.getChargeId()).isNull();
            assertThat(evento.isCharged()).isFalse();
        }
    }

    @Nested
    @DisplayName("attachToCharge")
    class AttachToCharge {

        @Test
        @DisplayName("cuelga el hecho del cargo conservando el resto")
        void cuelga_el_hecho_del_cargo_conservando_el_resto() {
            CompanyUsageEvent original = new CompanyUsageEvent(1L, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, null, CREADO, 2L);

            CompanyUsageEvent colgado = original.attachToCharge(CHARGE_ID);

            assertThat(colgado.getChargeId()).isEqualTo(CHARGE_ID);
            assertThat(colgado.getId()).isEqualTo(original.getId());
            assertThat(colgado.getVersion()).isEqualTo(original.getVersion());
            assertThat(colgado.isCharged()).isTrue();
            assertThat(original.isCharged()).isFalse();
        }

        @Test
        @DisplayName("chargeId es obligatorio")
        void charge_id_es_obligatorio() {
            CompanyUsageEvent original = new CompanyUsageEvent(1L, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, null, CREADO, 0L);

            assertThatThrownBy(() -> original.attachToCharge(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chargeId is required");
        }

        @Test
        @DisplayName("un hecho ya cobrado no se puede recolgar de otro cargo")
        void un_hecho_ya_cobrado_no_se_puede_recolgar() {
            CompanyUsageEvent yaCobrado = new CompanyUsageEvent(1L, COMPANY_ID, DIMENSION_ID,
                    UsageBranch.ANIMAL, ANIMAL_ID, OCCURRED_AT, PERIOD_KEY, true, CHARGE_ID, CREADO,
                    1L);

            assertThatThrownBy(() -> yaCobrado.attachToCharge(999L))
                    .isInstanceOf(UsageEventAlreadyChargedException.class)
                    .hasMessageContaining("is already attached to charge");
        }
    }
}
