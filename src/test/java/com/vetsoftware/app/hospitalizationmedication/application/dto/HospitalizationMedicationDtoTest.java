package com.vetsoftware.app.hospitalizationmedication.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.testsupport.HospitalizationMedicationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo compila, pasa cualquier test de "no es null", y solo se
 * ve en pantalla.
 */
@DisplayName("HospitalizationMedicationDto.from")
class HospitalizationMedicationDtoTest {

    @Test
    @DisplayName("copia cada campo escalar del agregado en su posicion")
    void copia_cada_campo_escalar_en_su_posicion() {
        HospitalizationMedication medication = HospitalizationMedicationMother.activo();

        HospitalizationMedicationDto dto = HospitalizationMedicationDto.from(medication);

        assertThat(dto.id()).isEqualTo(HospitalizationMedicationMother.MEDICATION_ID);
        assertThat(dto.name()).isEqualTo("Amoxicilina 500mg");
        assertThat(dto.dose()).isEqualTo("1 tableta");
        assertThat(dto.frequency()).isEqualTo("EVERY_8H");
        assertThat(dto.guidelineType()).isEqualTo("INTERVAL");
        assertThat(dto.durationMeasure()).isEqualTo("DAYS");
        assertThat(dto.durationQuantity()).isEqualTo(5);
        assertThat(dto.startDate()).isEqualTo(medication.getStartDate());
        assertThat(dto.startTime()).isEqualTo(medication.getStartTime());
        assertThat(dto.notes()).isEqualTo("Administrar con alimento");
        assertThat(dto.createdDate()).isEqualTo(HospitalizationMedicationMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("aplana la hospitalizacion y el creador en sus summaries")
    void aplana_la_hospitalizacion_y_el_creador_en_sus_summaries() {
        HospitalizationMedicationDto dto = HospitalizationMedicationDto
                .from(HospitalizationMedicationMother.activo());

        assertThat(dto.hospitalization()).isEqualTo(
                new HospitalizationSummaryDto(HospitalizationMedicationMother.HOSPITALIZACION.id(),
                        HospitalizationMedicationMother.HOSPITALIZACION.date()));
        assertThat(dto.createdBy())
                .isEqualTo(new EmployeeSummaryDto(HospitalizationMedicationMother.CREADO_POR.id(),
                        HospitalizationMedicationMother.CREADO_POR.employeeCode(),
                        HospitalizationMedicationMother.CREADO_POR.name()));
    }

    @Test
    @DisplayName("sin suspension, suspensionDate y suspensionBy quedan null")
    void sin_suspension_los_campos_de_suspension_quedan_null() {
        HospitalizationMedicationDto dto = HospitalizationMedicationDto
                .from(HospitalizationMedicationMother.activo());

        assertThat(dto.suspensionDate()).isNull();
        assertThat(dto.suspensionBy()).isNull();
    }

    @Test
    @DisplayName("con suspension, mapea tambien quien y cuando")
    void con_suspension_mapea_quien_y_cuando() {
        HospitalizationMedicationDto dto = HospitalizationMedicationDto
                .from(HospitalizationMedicationMother.suspendido());

        assertThat(dto.suspensionDate()).isEqualTo(HospitalizationMedicationMother.SUSPENDIDO_EL);
        assertThat(dto.suspensionBy()).isEqualTo(
                new EmployeeSummaryDto(HospitalizationMedicationMother.SUSPENDIDO_POR.id(),
                        HospitalizationMedicationMother.SUSPENDIDO_POR.employeeCode(),
                        HospitalizationMedicationMother.SUSPENDIDO_POR.name()));
    }

    @Test
    @DisplayName("propaga la orden deshabilitada")
    void propaga_la_orden_deshabilitada() {
        assertThat(HospitalizationMedicationDto
                .from(HospitalizationMedicationMother.deshabilitado()).enabled()).isFalse();
    }

    @Test
    @DisplayName("con el plan sin definir, los tres enums y la dosis/notas quedan null")
    void con_el_plan_sin_definir_los_campos_opcionales_quedan_null() {
        HospitalizationMedicationDto dto = HospitalizationMedicationDto
                .from(HospitalizationMedicationMother.sinDetallesOpcionales());

        assertThat(dto.dose()).isNull();
        assertThat(dto.frequency()).isNull();
        assertThat(dto.guidelineType()).isNull();
        assertThat(dto.durationMeasure()).isNull();
        assertThat(dto.durationQuantity()).isNull();
        assertThat(dto.startDate()).isNull();
        assertThat(dto.startTime()).isNull();
        assertThat(dto.notes()).isNull();
    }
}
