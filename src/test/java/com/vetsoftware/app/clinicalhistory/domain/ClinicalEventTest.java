package com.vetsoftware.app.clinicalhistory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ClinicalEvent — invariantes del evento clínico")
class ClinicalEventTest {

    private static final Long SOURCE_ID = 500L;
    private static final Long ANIMAL_ID = 100L;
    private static final Long COMPANY_ID = 9L;
    private static final LocalDate FECHA = LocalDate.of(2026, 8, 1);

    @Nested
    @DisplayName("construcción válida")
    class Creacion {

        @Test
        @DisplayName("acepta todos los campos, incluidos los opcionales")
        void acepta_todos_los_campos() {
            ClinicalEvent evento = new ClinicalEvent(SOURCE_ID, ANIMAL_ID, COMPANY_ID, SOURCE_ID,
                    FECHA, FECHA.plusDays(2), ClinicalEventType.HOSPITALIZATION, "Resumen");

            assertThat(evento.sourceId()).isEqualTo(SOURCE_ID);
            assertThat(evento.animalId()).isEqualTo(ANIMAL_ID);
            assertThat(evento.companyId()).isEqualTo(COMPANY_ID);
            assertThat(evento.consultationId()).isEqualTo(SOURCE_ID);
            assertThat(evento.eventDate()).isEqualTo(FECHA);
            assertThat(evento.endDate()).isEqualTo(FECHA.plusDays(2));
            assertThat(evento.eventType()).isEqualTo(ClinicalEventType.HOSPITALIZATION);
            assertThat(evento.summary()).isEqualTo("Resumen");
        }

        @Test
        @DisplayName("acepta los campos opcionales en null")
        void acepta_opcionales_en_null() {
            ClinicalEvent evento = new ClinicalEvent(SOURCE_ID, ANIMAL_ID, COMPANY_ID, null, FECHA,
                    null, ClinicalEventType.CONSULTATION, null);

            assertThat(evento.consultationId()).isNull();
            assertThat(evento.endDate()).isNull();
            assertThat(evento.summary()).isNull();
        }
    }

    @Nested
    @DisplayName("validaciones — campos obligatorios")
    class Validaciones {

        @Test
        @DisplayName("sourceId nulo se rechaza")
        void source_id_nulo_se_rechaza() {
            assertThatThrownBy(() -> new ClinicalEvent(null, ANIMAL_ID, COMPANY_ID, null, FECHA,
                    null, ClinicalEventType.CONSULTATION, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sourceId is required");
        }

        @Test
        @DisplayName("animalId nulo se rechaza")
        void animal_id_nulo_se_rechaza() {
            assertThatThrownBy(() -> new ClinicalEvent(SOURCE_ID, null, COMPANY_ID, null, FECHA,
                    null, ClinicalEventType.CONSULTATION, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animalId is required");
        }

        @Test
        @DisplayName("companyId nulo se rechaza")
        void company_id_nulo_se_rechaza() {
            assertThatThrownBy(() -> new ClinicalEvent(SOURCE_ID, ANIMAL_ID, null, null, FECHA,
                    null, ClinicalEventType.CONSULTATION, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("eventDate nula se rechaza")
        void event_date_nula_se_rechaza() {
            assertThatThrownBy(() -> new ClinicalEvent(SOURCE_ID, ANIMAL_ID, COMPANY_ID, null, null,
                    null, ClinicalEventType.CONSULTATION, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eventDate is required");
        }

        @Test
        @DisplayName("eventType nulo se rechaza")
        void event_type_nulo_se_rechaza() {
            assertThatThrownBy(() -> new ClinicalEvent(SOURCE_ID, ANIMAL_ID, COMPANY_ID, null,
                    FECHA, null, null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eventType is required");
        }
    }
}
