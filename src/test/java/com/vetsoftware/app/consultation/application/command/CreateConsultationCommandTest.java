package com.vetsoftware.app.consultation.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CreateConsultationCommand")
class CreateConsultationCommandTest {

    @Test
    @DisplayName("el constructor canonico conserva cada campo, incluido el peso opcional")
    void el_constructor_canonico_conserva_cada_campo() {
        CreateConsultationCommand command = ConsultationMother
                .comandoCrearConPeso(new BigDecimal("12.50"), "KILOGRAMS");

        assertThat(command.date()).isEqualTo(ConsultationMother.FECHA);
        assertThat(command.consultationTypeId()).isEqualTo(ConsultationMother.CONSULTATION_TYPE_ID);
        assertThat(command.anamnesis()).isEqualTo("Anamnesis del paciente");
        assertThat(command.diagnosis()).isEqualTo("Diagnostico");
        assertThat(command.prognosis()).isEqualTo("Pronostico reservado");
        assertThat(command.nextControl()).isEqualTo(ConsultationMother.FECHA.plusDays(15));
        assertThat(command.animalId()).isEqualTo(ConsultationMother.ANIMAL_ID);
        assertThat(command.companyId()).isEqualTo(ConsultationMother.COMPANY_ID);
        assertThat(command.weight()).isEqualByComparingTo("12.50");
        assertThat(command.weightUnit()).isEqualTo("KILOGRAMS");
        assertThat(command.temperature()).isEqualByComparingTo("38.5");
        assertThat(command.heartRate()).isEqualTo(90);
        assertThat(command.respiratoryRate()).isEqualTo(22);
        assertThat(command.mucousMembranes()).isEqualTo("Rosadas");
        assertThat(command.capillaryRefill()).isEqualTo("< 2 seg");
        assertThat(command.hydration()).isEqualTo("Normal");
        assertThat(command.bodyConditionScore()).isEqualTo(5);
        assertThat(command.painScore()).isEqualTo(2);
        assertThat(command.attitude()).isEqualTo("Alerta");
        assertThat(command.examFindings()).isEqualTo("Sin hallazgos relevantes");
    }

    @Test
    @DisplayName("el peso es opcional: viaja en null cuando la consulta no lo registra")
    void el_peso_es_opcional() {
        CreateConsultationCommand command = ConsultationMother.comandoCrear();

        assertThat(command.weight()).isNull();
        assertThat(command.weightUnit()).isNull();
    }
}
