package com.vetsoftware.app.medicationschedule.infrastructure.persistence;

import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.CREADO;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.EMPLEADO;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.ORDEN;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.SCHEDULE_ID;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.tomaPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaEntity;
import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.EmployeeRef;
import com.vetsoftware.app.medicationschedule.domain.HospitalizationMedicationRef;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El unico cruce real de este mapper es {@code appliedStatus}: viaja como
 * String plano en la tabla y como enum en el dominio, y el ternario que hace la
 * conversion en cada sentido puede quedarse en null. Las entidades JPA de otras
 * features se mockean porque su constructor sin argumentos es {@code protected}
 * y no son instanciables desde este paquete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationScheduleJpaMapper")
class MedicationScheduleJpaMapperTest {

    private final MedicationScheduleJpaMapper mapper = new MedicationScheduleJpaMapper();

    @Mock
    private HospitalizationMedicationJpaEntity hospitalizationMedicationEntity;
    @Mock
    private EmployeeJpaEntity employeeEntity;

    private static MedicationScheduleJpaEntity entidadCompleta() {
        MedicationScheduleJpaEntity entity = new MedicationScheduleJpaEntity();
        entity.setId(SCHEDULE_ID);
        entity.setOriginalDateTime(PRIMERA_TOMA);
        entity.setCurrentDateTime(PRIMERA_TOMA);
        entity.setRealDateTime(null);
        entity.setAppliedStatus("PENDING");
        entity.setRescheduled(false);
        entity.setCreatedDate(CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            MedicationScheduleJpaEntity entity = mapper.toJpa(tomaPendiente(),
                    hospitalizationMedicationEntity, employeeEntity);

            assertThat(entity.getId()).isEqualTo(SCHEDULE_ID);
            assertThat(entity.getOriginalDateTime()).isEqualTo(PRIMERA_TOMA);
            assertThat(entity.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA);
            assertThat(entity.getRealDateTime()).isNull();
            assertThat(entity.getAppliedStatus()).isEqualTo("PENDING");
            assertThat(entity.getRescheduled()).isFalse();
            assertThat(entity.getCreatedDate()).isEqualTo(CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha cada asociacion en su slot")
        void engancha_cada_asociacion_en_su_slot() {
            MedicationScheduleJpaEntity entity = mapper.toJpa(tomaPendiente(),
                    hospitalizationMedicationEntity, employeeEntity);

            assertThat(entity.getHospitalizationMedication())
                    .isSameAs(hospitalizationMedicationEntity);
            assertThat(entity.getCreatedBy()).isSameAs(employeeEntity);
        }

        @Test
        @DisplayName("una toma sin estado aplicado guarda la columna en null, no la cadena \"null\"")
        void una_toma_sin_estado_guarda_null() {
            MedicationSchedule sinEstado = new MedicationSchedule(SCHEDULE_ID, ORDEN, PRIMERA_TOMA,
                    PRIMERA_TOMA, null, null, false, EMPLEADO, CREADO, true);

            MedicationScheduleJpaEntity entity = mapper.toJpa(sinEstado,
                    hospitalizationMedicationEntity, employeeEntity);

            assertThat(entity.getAppliedStatus()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            MedicationSchedule domain = mapper.toDomain(entidadCompleta(), ORDEN, EMPLEADO);

            assertThat(domain.getId()).isEqualTo(SCHEDULE_ID);
            assertThat(domain.getHospitalizationMedication()).isEqualTo(ORDEN);
            assertThat(domain.getCreatedBy()).isEqualTo(EMPLEADO);
            assertThat(domain.getOriginalDateTime()).isEqualTo(PRIMERA_TOMA);
            assertThat(domain.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA);
            assertThat(domain.getAppliedStatus()).isEqualTo(AppliedStatus.PENDING);
            assertThat(domain.getRescheduled()).isFalse();
            assertThat(domain.getCreatedDate()).isEqualTo(CREADO);
            assertThat(domain.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("una columna appliedStatus en null se mapea a estado null, no revienta el enum")
        void una_columna_null_se_mapea_a_estado_null() {
            MedicationScheduleJpaEntity entity = entidadCompleta();
            entity.setAppliedStatus(null);

            MedicationSchedule domain = mapper.toDomain(entity, ORDEN, EMPLEADO);

            assertThat(domain.getAppliedStatus()).isNull();
        }

        @Test
        @DisplayName("la ida y vuelta dominio a entidad a dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            MedicationSchedule original = tomaPendiente();

            MedicationScheduleJpaEntity entity = mapper.toJpa(original,
                    hospitalizationMedicationEntity, employeeEntity);
            MedicationSchedule vuelta = mapper.toDomain(entity,
                    original.getHospitalizationMedication(), original.getCreatedBy());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(hospitalizationMedicationEntity.getId()).thenReturn(ORDEN.id());
            when(hospitalizationMedicationEntity.getName()).thenReturn(ORDEN.name());
            when(employeeEntity.getId()).thenReturn(EMPLEADO.id());
            when(employeeEntity.getEmployeeCode()).thenReturn(EMPLEADO.employeeCode());
            when(employeeEntity.getName()).thenReturn(EMPLEADO.name());

            MedicationScheduleJpaEntity entity = entidadCompleta();
            entity.setHospitalizationMedication(hospitalizationMedicationEntity);
            entity.setCreatedBy(employeeEntity);

            MedicationSchedule domain = mapper.toDomain(entity);

            assertThat(domain.getHospitalizationMedication())
                    .isEqualTo(new HospitalizationMedicationRef(ORDEN.id(), ORDEN.name()));
            assertThat(domain.getCreatedBy()).isEqualTo(
                    new EmployeeRef(EMPLEADO.id(), EMPLEADO.employeeCode(), EMPLEADO.name()));
        }
    }
}
