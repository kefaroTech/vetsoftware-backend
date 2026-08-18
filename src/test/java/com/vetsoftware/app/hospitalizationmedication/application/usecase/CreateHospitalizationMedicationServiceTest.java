package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationmedication.application.command.CreateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationmedication.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationmedication.domain.Frequency;
import com.vetsoftware.app.hospitalizationmedication.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.testsupport.HospitalizationMedicationMother;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateHospitalizationMedicationService")
class CreateHospitalizationMedicationServiceTest {

    @Mock
    private HospitalizationMedicationRepository repository;
    @Mock
    private HospitalizationQueryPort hospitalizationQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;

    @InjectMocks
    private CreateHospitalizationMedicationService service;

    @Captor
    private ArgumentCaptor<HospitalizationMedication> captor;

    private void referenciasResueltas() {
        when(hospitalizationQueryPort.findById(HospitalizationMedicationMother.HOSPITALIZATION_ID))
                .thenReturn(Optional.of(HospitalizationMedicationMother.HOSPITALIZACION));
        when(employeeQueryPort.findById(HospitalizationMedicationMother.EMPLOYEE_ID))
                .thenReturn(Optional.of(HospitalizationMedicationMother.CREADO_POR));
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la orden con las referencias resueltas por los puertos")
        void persiste_la_orden_con_las_referencias_resueltas() {
            referenciasResueltas();
            when(repository.save(any())).thenReturn(HospitalizationMedicationMother.activo());

            service.execute(HospitalizationMedicationMother.comandoCrear());

            verify(repository).save(captor.capture());
            HospitalizationMedication guardado = captor.getValue();
            assertThat(guardado.getHospitalization())
                    .isEqualTo(HospitalizationMedicationMother.HOSPITALIZACION);
            assertThat(guardado.getCreatedBy())
                    .isEqualTo(HospitalizationMedicationMother.CREADO_POR);
            assertThat(guardado.getName()).isEqualTo("Amoxicilina 500mg");
            assertThat(guardado.getFrequency()).isEqualTo(Frequency.EVERY_8H);
            assertThat(guardado.getGuidelineType()).isEqualTo(GuidelineType.INTERVAL);
            assertThat(guardado.getDurationMeasure()).isEqualTo(DurationMeasure.DAYS);
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("los enums llegan en minusculas y con espacios: se normalizan igual")
        void los_enums_llegan_en_minusculas_y_con_espacios() {
            referenciasResueltas();
            when(repository.save(any())).thenReturn(HospitalizationMedicationMother.activo());
            CreateHospitalizationMedicationCommand comando = new CreateHospitalizationMedicationCommand(
                    "Amoxicilina 500mg", "1 tableta", " every_8h ", " interval ", " days ", 5,
                    LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Notas",
                    HospitalizationMedicationMother.HOSPITALIZATION_ID,
                    HospitalizationMedicationMother.EMPLOYEE_ID);

            service.execute(comando);

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getFrequency()).isEqualTo(Frequency.EVERY_8H);
            assertThat(captor.getValue().getGuidelineType()).isEqualTo(GuidelineType.INTERVAL);
            assertThat(captor.getValue().getDurationMeasure()).isEqualTo(DurationMeasure.DAYS);
        }

        @Test
        @DisplayName("planificacion nula se mapea a null en los tres enums")
        void planificacion_nula_se_mapea_a_null() {
            referenciasResueltas();
            when(repository.save(any())).thenReturn(HospitalizationMedicationMother.activo());
            CreateHospitalizationMedicationCommand comando = new CreateHospitalizationMedicationCommand(
                    "Amoxicilina 500mg", null, null, null, null, null, null, null, null,
                    HospitalizationMedicationMother.HOSPITALIZATION_ID,
                    HospitalizationMedicationMother.EMPLOYEE_ID);

            service.execute(comando);

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getFrequency()).isNull();
            assertThat(captor.getValue().getGuidelineType()).isNull();
            assertThat(captor.getValue().getDurationMeasure()).isNull();
        }

        @Test
        @DisplayName("planificacion en blanco tambien se mapea a null")
        void planificacion_en_blanco_se_mapea_a_null() {
            referenciasResueltas();
            when(repository.save(any())).thenReturn(HospitalizationMedicationMother.activo());
            CreateHospitalizationMedicationCommand comando = new CreateHospitalizationMedicationCommand(
                    "Amoxicilina 500mg", null, "   ", "   ", "   ", null, null, null, null,
                    HospitalizationMedicationMother.HOSPITALIZATION_ID,
                    HospitalizationMedicationMother.EMPLOYEE_ID);

            service.execute(comando);

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getFrequency()).isNull();
            assertThat(captor.getValue().getGuidelineType()).isNull();
            assertThat(captor.getValue().getDurationMeasure()).isNull();
        }

        @Test
        @DisplayName("devuelve el DTO de la orden ya persistida")
        void devuelve_el_dto_de_la_orden_ya_persistida() {
            referenciasResueltas();
            when(repository.save(any())).thenReturn(HospitalizationMedicationMother.activo());

            HospitalizationMedicationDto dto = service
                    .execute(HospitalizationMedicationMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(HospitalizationMedicationMother.MEDICATION_ID);
            assertThat(dto.name()).isEqualTo("Amoxicilina 500mg");
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("hospitalizacion inexistente: no consulta al empleado ni persiste")
        void hospitalizacion_inexistente() {
            when(hospitalizationQueryPort
                    .findById(HospitalizationMedicationMother.HOSPITALIZATION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationMedicationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Hospitalization not found: "
                            + HospitalizationMedicationMother.HOSPITALIZATION_ID);

            verifyNoInteractions(employeeQueryPort, repository);
        }

        @Test
        @DisplayName("empleado inexistente: no persiste")
        void empleado_inexistente() {
            when(hospitalizationQueryPort
                    .findById(HospitalizationMedicationMother.HOSPITALIZATION_ID))
                    .thenReturn(Optional.of(HospitalizationMedicationMother.HOSPITALIZACION));
            when(employeeQueryPort.findById(HospitalizationMedicationMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationMedicationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Employee not found: " + HospitalizationMedicationMother.EMPLOYEE_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un nombre vacio no llega a persistirse aunque las referencias existan")
        void un_nombre_vacio_no_llega_a_persistirse() {
            referenciasResueltas();
            CreateHospitalizationMedicationCommand comando = new CreateHospitalizationMedicationCommand(
                    "   ", "1 tableta", "EVERY_8H", "INTERVAL", "DAYS", 5, LocalDate.of(2026, 3, 1),
                    LocalTime.of(8, 0), "Notas", HospitalizationMedicationMother.HOSPITALIZATION_ID,
                    HospitalizationMedicationMother.EMPLOYEE_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }
    }
}
