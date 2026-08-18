package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationprocedure.application.command.CreateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationprocedure.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationprocedure.domain.Frequency;
import com.vetsoftware.app.hospitalizationprocedure.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.testsupport.HospitalizationProcedureMother;
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
@DisplayName("CreateHospitalizationProcedureService")
class CreateHospitalizationProcedureServiceTest {

    @Mock
    private HospitalizationProcedureRepository repository;
    @Mock
    private HospitalizationQueryPort hospitalizationQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;

    @InjectMocks
    private CreateHospitalizationProcedureService service;

    @Captor
    private ArgumentCaptor<HospitalizationProcedure> captor;

    private void referenciasResueltas() {
        when(hospitalizationQueryPort.findById(HospitalizationProcedureMother.HOSPITALIZATION_ID))
                .thenReturn(Optional.of(HospitalizationProcedureMother.HOSPITALIZACION));
        when(employeeQueryPort.findById(HospitalizationProcedureMother.EMPLOYEE_ID))
                .thenReturn(Optional.of(HospitalizationProcedureMother.CREADO_POR));
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la orden con las referencias resueltas por los puertos")
        void persiste_la_orden_con_las_referencias_resueltas() {
            referenciasResueltas();
            when(repository.save(any())).thenReturn(HospitalizationProcedureMother.activo());

            service.execute(HospitalizationProcedureMother.comandoCrear());

            verify(repository).save(captor.capture());
            HospitalizationProcedure guardado = captor.getValue();
            assertThat(guardado.getHospitalization())
                    .isEqualTo(HospitalizationProcedureMother.HOSPITALIZACION);
            assertThat(guardado.getCreatedBy())
                    .isEqualTo(HospitalizationProcedureMother.CREADO_POR);
            assertThat(guardado.getName()).isEqualTo("Curacion de herida");
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
            when(repository.save(any())).thenReturn(HospitalizationProcedureMother.activo());
            CreateHospitalizationProcedureCommand comando = new CreateHospitalizationProcedureCommand(
                    "Curacion de herida", "Solucion salina 0.9%", " every_8h ", " interval ",
                    " days ", 5, LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Notas",
                    HospitalizationProcedureMother.HOSPITALIZATION_ID,
                    HospitalizationProcedureMother.EMPLOYEE_ID);

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
            when(repository.save(any())).thenReturn(HospitalizationProcedureMother.activo());
            CreateHospitalizationProcedureCommand comando = new CreateHospitalizationProcedureCommand(
                    "Curacion de herida", null, null, null, null, null, null, null, null,
                    HospitalizationProcedureMother.HOSPITALIZATION_ID,
                    HospitalizationProcedureMother.EMPLOYEE_ID);

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
            when(repository.save(any())).thenReturn(HospitalizationProcedureMother.activo());
            CreateHospitalizationProcedureCommand comando = new CreateHospitalizationProcedureCommand(
                    "Curacion de herida", null, "   ", "   ", "   ", null, null, null, null,
                    HospitalizationProcedureMother.HOSPITALIZATION_ID,
                    HospitalizationProcedureMother.EMPLOYEE_ID);

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
            when(repository.save(any())).thenReturn(HospitalizationProcedureMother.activo());

            HospitalizationProcedureDto dto = service
                    .execute(HospitalizationProcedureMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(HospitalizationProcedureMother.PROCEDURE_ID);
            assertThat(dto.name()).isEqualTo("Curacion de herida");
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("hospitalizacion inexistente: no consulta al empleado ni persiste")
        void hospitalizacion_inexistente() {
            when(hospitalizationQueryPort
                    .findById(HospitalizationProcedureMother.HOSPITALIZATION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationProcedureMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Hospitalization not found: "
                            + HospitalizationProcedureMother.HOSPITALIZATION_ID);

            verifyNoInteractions(employeeQueryPort, repository);
        }

        @Test
        @DisplayName("empleado inexistente: no persiste")
        void empleado_inexistente() {
            when(hospitalizationQueryPort
                    .findById(HospitalizationProcedureMother.HOSPITALIZATION_ID))
                    .thenReturn(Optional.of(HospitalizationProcedureMother.HOSPITALIZACION));
            when(employeeQueryPort.findById(HospitalizationProcedureMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationProcedureMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Employee not found: " + HospitalizationProcedureMother.EMPLOYEE_ID);

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
            CreateHospitalizationProcedureCommand comando = new CreateHospitalizationProcedureCommand(
                    "   ", "Solucion salina 0.9%", "EVERY_8H", "INTERVAL", "DAYS", 5,
                    LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Notas",
                    HospitalizationProcedureMother.HOSPITALIZATION_ID,
                    HospitalizationProcedureMother.EMPLOYEE_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            verify(repository, never()).save(any());
        }
    }
}
