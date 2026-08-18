package com.vetsoftware.app.procedureschedule.application.dto;

import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.CREADO;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.EMPLEADO;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.ORDEN;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.SCHEDULE_ID;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.tomaPendiente;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.procedureschedule.domain.AppliedStatus;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El unico riesgo real de este mapeo campo a campo es el estado aplicado: viaja
 * como String plano y {@code from} lo aplana con un ternario que puede quedarse
 * en null. El resto de campos son copia directa.
 */
@DisplayName("ProcedureScheduleDto.from")
class ProcedureScheduleDtoTest {

    @Test
    @DisplayName("copia cada campo de la toma en su posicion")
    void copia_cada_campo_de_la_toma_en_su_posicion() {
        ProcedureScheduleDto dto = ProcedureScheduleDto.from(tomaPendiente());

        assertThat(dto.id()).isEqualTo(SCHEDULE_ID);
        assertThat(dto.hospitalizationProcedureId()).isEqualTo(ORDEN.id());
        assertThat(dto.hospitalizationProcedureName()).isEqualTo(ORDEN.name());
        assertThat(dto.originalDateTime()).isEqualTo(PRIMERA_TOMA);
        assertThat(dto.currentDateTime()).isEqualTo(PRIMERA_TOMA);
        assertThat(dto.realDateTime()).isNull();
        assertThat(dto.appliedStatus()).isEqualTo("PENDING");
        assertThat(dto.rescheduled()).isFalse();
        assertThat(dto.createdById()).isEqualTo(EMPLEADO.id());
        assertThat(dto.createdByCode()).isEqualTo(EMPLEADO.employeeCode());
        assertThat(dto.createdByName()).isEqualTo(EMPLEADO.name());
        assertThat(dto.createdDate()).isEqualTo(CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("una toma sin estado aplicado se mapea a appliedStatus null, no a la cadena null")
    void una_toma_sin_estado_se_mapea_a_null() {
        ProcedureSchedule sinEstado = new ProcedureSchedule(SCHEDULE_ID, ORDEN, PRIMERA_TOMA,
                PRIMERA_TOMA, null, null, false, EMPLEADO, CREADO, true);

        assertThat(ProcedureScheduleDto.from(sinEstado).appliedStatus()).isNull();
    }

    @Test
    @DisplayName("una toma aplicada propaga la hora real")
    void una_toma_aplicada_propaga_la_hora_real() {
        ProcedureSchedule aplicada = new ProcedureSchedule(SCHEDULE_ID, ORDEN, PRIMERA_TOMA,
                PRIMERA_TOMA, PRIMERA_TOMA.plusMinutes(10), AppliedStatus.APPLIED, false, EMPLEADO,
                CREADO, true);

        ProcedureScheduleDto dto = ProcedureScheduleDto.from(aplicada);

        assertThat(dto.appliedStatus()).isEqualTo("APPLIED");
        assertThat(dto.realDateTime()).isEqualTo(PRIMERA_TOMA.plusMinutes(10));
    }
}
