package com.vetsoftware.app.medicationschedule.application.dto;

import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.CREADO;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.EMPLEADO;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.ORDEN;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.SCHEDULE_ID;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.tomaPendiente;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El unico riesgo real de este mapeo campo a campo es el estado aplicado: viaja
 * como String plano y {@code from} lo aplana con un ternario que puede quedarse
 * en null. El resto de campos son copia directa.
 */
@DisplayName("MedicationScheduleDto.from")
class MedicationScheduleDtoTest {

    @Test
    @DisplayName("copia cada campo de la toma en su posicion")
    void copia_cada_campo_de_la_toma_en_su_posicion() {
        MedicationScheduleDto dto = MedicationScheduleDto.from(tomaPendiente());

        assertThat(dto.id()).isEqualTo(SCHEDULE_ID);
        assertThat(dto.hospitalizationMedicationId()).isEqualTo(ORDEN.id());
        assertThat(dto.hospitalizationMedicationName()).isEqualTo(ORDEN.name());
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
        MedicationSchedule sinEstado = new MedicationSchedule(SCHEDULE_ID, ORDEN, PRIMERA_TOMA,
                PRIMERA_TOMA, null, null, false, EMPLEADO, CREADO, null, true);

        assertThat(MedicationScheduleDto.from(sinEstado).appliedStatus()).isNull();
    }

    @Test
    @DisplayName("una toma aplicada propaga la hora real")
    void una_toma_aplicada_propaga_la_hora_real() {
        MedicationSchedule aplicada = new MedicationSchedule(SCHEDULE_ID, ORDEN, PRIMERA_TOMA,
                PRIMERA_TOMA, PRIMERA_TOMA.plusMinutes(10), AppliedStatus.APPLIED, false, EMPLEADO,
                CREADO, null, true);

        MedicationScheduleDto dto = MedicationScheduleDto.from(aplicada);

        assertThat(dto.appliedStatus()).isEqualTo("APPLIED");
        assertThat(dto.realDateTime()).isEqualTo(PRIMERA_TOMA.plusMinutes(10));
    }
}
