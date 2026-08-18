package com.vetsoftware.app.hospitalizationprocedure.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.testsupport.HospitalizationProcedureMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo compila, pasa cualquier test de "no es null", y solo se
 * ve en pantalla.
 */
@DisplayName("HospitalizationProcedureDto.from")
class HospitalizationProcedureDtoTest {

    @Test
    @DisplayName("copia cada campo escalar del agregado en su posicion")
    void copia_cada_campo_escalar_en_su_posicion() {
        HospitalizationProcedure procedure = HospitalizationProcedureMother.activo();

        HospitalizationProcedureDto dto = HospitalizationProcedureDto.from(procedure);

        assertThat(dto.id()).isEqualTo(HospitalizationProcedureMother.PROCEDURE_ID);
        assertThat(dto.name()).isEqualTo("Curacion de herida");
        assertThat(dto.dose()).isEqualTo("Solucion salina 0.9%");
        assertThat(dto.frequency()).isEqualTo("EVERY_8H");
        assertThat(dto.guidelineType()).isEqualTo("INTERVAL");
        assertThat(dto.durationMeasure()).isEqualTo("DAYS");
        assertThat(dto.durationQuantity()).isEqualTo(5);
        assertThat(dto.startDate()).isEqualTo(procedure.getStartDate());
        assertThat(dto.startTime()).isEqualTo(procedure.getStartTime());
        assertThat(dto.notes()).isEqualTo("Cambiar el vendaje cada turno");
        assertThat(dto.createdDate()).isEqualTo(HospitalizationProcedureMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("aplana la hospitalizacion y el creador en sus summaries")
    void aplana_la_hospitalizacion_y_el_creador_en_sus_summaries() {
        HospitalizationProcedureDto dto = HospitalizationProcedureDto
                .from(HospitalizationProcedureMother.activo());

        assertThat(dto.hospitalization()).isEqualTo(
                new HospitalizationSummaryDto(HospitalizationProcedureMother.HOSPITALIZACION.id(),
                        HospitalizationProcedureMother.HOSPITALIZACION.date()));
        assertThat(dto.createdBy())
                .isEqualTo(new EmployeeSummaryDto(HospitalizationProcedureMother.CREADO_POR.id(),
                        HospitalizationProcedureMother.CREADO_POR.employeeCode(),
                        HospitalizationProcedureMother.CREADO_POR.name()));
    }

    @Test
    @DisplayName("sin suspension, suspensionDate y suspensionBy quedan null")
    void sin_suspension_los_campos_de_suspension_quedan_null() {
        HospitalizationProcedureDto dto = HospitalizationProcedureDto
                .from(HospitalizationProcedureMother.activo());

        assertThat(dto.suspensionDate()).isNull();
        assertThat(dto.suspensionBy()).isNull();
    }

    @Test
    @DisplayName("con suspension, mapea tambien quien y cuando")
    void con_suspension_mapea_quien_y_cuando() {
        HospitalizationProcedureDto dto = HospitalizationProcedureDto
                .from(HospitalizationProcedureMother.suspendido());

        assertThat(dto.suspensionDate()).isEqualTo(HospitalizationProcedureMother.SUSPENDIDO_EL);
        assertThat(dto.suspensionBy()).isEqualTo(
                new EmployeeSummaryDto(HospitalizationProcedureMother.SUSPENDIDO_POR.id(),
                        HospitalizationProcedureMother.SUSPENDIDO_POR.employeeCode(),
                        HospitalizationProcedureMother.SUSPENDIDO_POR.name()));
    }

    @Test
    @DisplayName("propaga la orden deshabilitada")
    void propaga_la_orden_deshabilitada() {
        assertThat(HospitalizationProcedureDto.from(HospitalizationProcedureMother.deshabilitado())
                .enabled()).isFalse();
    }

    @Test
    @DisplayName("con el plan sin definir, los tres enums y la dosis/notas quedan null")
    void con_el_plan_sin_definir_los_campos_opcionales_quedan_null() {
        HospitalizationProcedureDto dto = HospitalizationProcedureDto
                .from(HospitalizationProcedureMother.sinDetallesOpcionales());

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
