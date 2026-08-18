package com.vetsoftware.app.clinicalhistory.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GetClinicalHistoryQuery — invariantes y normalización")
class GetClinicalHistoryQueryTest {

    private static final Long ANIMAL_ID = 100L;
    private static final Long COMPANY_ID = 9L;

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("animalId nulo se rechaza")
        void animal_id_nulo_se_rechaza() {
            assertThatThrownBy(() -> new GetClinicalHistoryQuery(null, COMPANY_ID, List.of(), null,
                    null, null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animalId is required");
        }

        @Test
        @DisplayName("companyId nulo se rechaza")
        void company_id_nulo_se_rechaza() {
            assertThatThrownBy(() -> new GetClinicalHistoryQuery(ANIMAL_ID, null, List.of(), null,
                    null, null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("'to' anterior a 'from' se rechaza")
        void to_anterior_a_from_se_rechaza() {
            LocalDate from = LocalDate.of(2026, 8, 10);
            LocalDate to = LocalDate.of(2026, 8, 1);

            assertThatThrownBy(() -> new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID, List.of(),
                    from, to, null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'to' cannot be before 'from'");
        }

        @Test
        @DisplayName("'from' y 'to' iguales es un rango válido")
        void from_y_to_iguales_es_valido() {
            LocalDate fecha = LocalDate.of(2026, 8, 10);

            GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID,
                    List.of(), fecha, fecha, null, null);

            assertThat(query.from()).isEqualTo(fecha);
            assertThat(query.to()).isEqualTo(fecha);
        }
    }

    @Nested
    @DisplayName("normalización")
    class Normalizacion {

        @Test
        @DisplayName("types nulo se normaliza a lista vacía")
        void types_nulo_se_normaliza_a_lista_vacia() {
            GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID, null,
                    null, null, null, null);

            assertThat(query.types()).isEmpty();
        }

        @Test
        @DisplayName("q en blanco se normaliza a null")
        void q_en_blanco_se_normaliza_a_null() {
            GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID,
                    List.of(), null, null, "   ", null);

            assertThat(query.q()).isNull();
        }

        @Test
        @DisplayName("q nulo se conserva null")
        void q_nulo_se_conserva_null() {
            GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID,
                    List.of(), null, null, null, null);

            assertThat(query.q()).isNull();
        }

        @Test
        @DisplayName("q con espacios sobrantes se recorta")
        void q_con_espacios_se_recorta() {
            GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID,
                    List.of(), null, null, "  otitis  ", null);

            assertThat(query.q()).isEqualTo("otitis");
        }

        @Test
        @DisplayName("solo from, o solo to, es un rango válido")
        void solo_un_extremo_es_valido() {
            LocalDate from = LocalDate.of(2026, 8, 1);
            LocalDate to = LocalDate.of(2026, 8, 31);

            GetClinicalHistoryQuery soloFrom = new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID,
                    List.of(), from, null, null, null);
            GetClinicalHistoryQuery soloTo = new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID,
                    List.of(), null, to, null, null);

            assertThat(soloFrom.to()).isNull();
            assertThat(soloTo.from()).isNull();
        }

        @Test
        @DisplayName("types con tipos filtra y consultationId se conserva tal cual")
        void types_y_consultation_id_se_conservan() {
            GetClinicalHistoryQuery query = new GetClinicalHistoryQuery(ANIMAL_ID, COMPANY_ID,
                    List.of(ClinicalEventType.SURGERY, ClinicalEventType.CONSULTATION), null, null,
                    null, 42L);

            assertThat(query.types()).containsExactly(ClinicalEventType.SURGERY,
                    ClinicalEventType.CONSULTATION);
            assertThat(query.consultationId()).isEqualTo(42L);
        }
    }
}
