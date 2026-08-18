package com.vetsoftware.app.consultation.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo (por ejemplo {@code diagnosis}/{@code prognosis})
 * compila, pasa cualquier test de "no es null", y solo se ve en pantalla.
 */
@DisplayName("ConsultationDto.from")
class ConsultationDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        Consultation consultation = ConsultationMother.consultaValida();

        ConsultationDto dto = ConsultationDto.from(consultation);

        assertThat(dto.id()).isEqualTo(ConsultationMother.CONSULTATION_ID);
        assertThat(dto.date()).isEqualTo(ConsultationMother.FECHA);
        assertThat(dto.anamnesis()).isEqualTo("Anamnesis del paciente");
        assertThat(dto.diagnosis()).isEqualTo("Diagnostico");
        assertThat(dto.prognosis()).isEqualTo("Pronostico reservado");
        assertThat(dto.nextControl()).isEqualTo(ConsultationMother.FECHA.plusDays(15));
        assertThat(dto.createdDate()).isEqualTo(ConsultationMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("aplana los companion VO en summaries sin perder campos")
    void aplana_los_companion_vo_en_summaries() {
        ConsultationDto dto = ConsultationDto.from(ConsultationMother.consultaValida());

        assertThat(dto.consultationType()).isEqualTo(new ConsultationTypeSummaryDto(
                ConsultationMother.CONTROL.id(), ConsultationMother.CONTROL.name()));
        assertThat(dto.animal()).isEqualTo(new AnimalSummaryDto(ConsultationMother.FIRULAIS.id(),
                ConsultationMother.FIRULAIS.name(), ConsultationMother.FIRULAIS.code()));
        assertThat(dto.company()).isEqualTo(new CompanySummaryDto(ConsultationMother.CLINICA.id(),
                ConsultationMother.CLINICA.name(), ConsultationMother.CLINICA.identifier()));
    }

    @Nested
    @DisplayName("examen fisico aplanado")
    class ExamenFisicoAplanado {

        @Test
        @DisplayName("con examen completo, cada constante vital viaja en su propio campo")
        void con_examen_completo_cada_constante_vital_viaja_en_su_propio_campo() {
            ConsultationDto dto = ConsultationDto.from(ConsultationMother.consultaValida());

            assertThat(dto.temperature()).isEqualByComparingTo("38.5");
            assertThat(dto.heartRate()).isEqualTo(90);
            assertThat(dto.respiratoryRate()).isEqualTo(22);
            assertThat(dto.mucousMembranes()).isEqualTo("Rosadas");
            assertThat(dto.capillaryRefill()).isEqualTo("< 2 seg");
            assertThat(dto.hydration()).isEqualTo("Normal");
            assertThat(dto.bodyConditionScore()).isEqualTo(5);
            assertThat(dto.painScore()).isEqualTo(2);
            assertThat(dto.attitude()).isEqualTo("Alerta");
            assertThat(dto.examFindings()).isEqualTo("Sin hallazgos relevantes");
        }

        @Test
        @DisplayName("sin examen registrado, todos los campos del examen viajan en null")
        void sin_examen_registrado_todos_los_campos_viajan_en_null() {
            ConsultationDto dto = ConsultationDto.from(ConsultationMother.sinExamen());

            assertThat(dto.temperature()).isNull();
            assertThat(dto.heartRate()).isNull();
            assertThat(dto.respiratoryRate()).isNull();
            assertThat(dto.mucousMembranes()).isNull();
            assertThat(dto.bodyConditionScore()).isNull();
            assertThat(dto.painScore()).isNull();
            assertThat(dto.attitude()).isNull();
            assertThat(dto.examFindings()).isNull();
        }
    }

    @Test
    @DisplayName("propaga la consulta deshabilitada")
    void propaga_la_consulta_deshabilitada() {
        assertThat(ConsultationDto.from(ConsultationMother.deshabilitada()).enabled()).isFalse();
    }
}
