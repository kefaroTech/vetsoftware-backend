package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.testsupport.HospitalizationObservationMother;
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
@DisplayName("CreateHospitalizationObservationService")
class CreateHospitalizationObservationServiceTest {

    @Mock
    private HospitalizationObservationRepository repository;
    @Mock
    private HospitalizationQueryPort hospitalizationQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;

    @InjectMocks
    private CreateHospitalizationObservationService service;

    @Captor
    private ArgumentCaptor<HospitalizationObservation> observationCaptor;

    private void referenciasResueltas() {
        when(hospitalizationQueryPort.findById(HospitalizationObservationMother.HOSPITALIZATION_ID))
                .thenReturn(Optional.of(HospitalizationObservationMother.HOSPITALIZACION));
        when(employeeQueryPort.findById(HospitalizationObservationMother.EMPLOYEE_ID))
                .thenReturn(Optional.of(HospitalizationObservationMother.VETERINARIO));
    }

    @Nested
    @DisplayName("creacion valida")
    class CreacionValida {

        @Test
        @DisplayName("persiste la observacion con la hospitalizacion y el creador resueltos por los puertos")
        void persiste_con_las_referencias_resueltas_por_los_puertos() {
            referenciasResueltas();
            when(repository.save(any()))
                    .thenReturn(HospitalizationObservationMother.observacionValida());

            service.execute(HospitalizationObservationMother.comandoCrear());

            verify(repository).save(observationCaptor.capture());
            HospitalizationObservation guardada = observationCaptor.getValue();
            assertThat(guardada.getHospitalization())
                    .isEqualTo(HospitalizationObservationMother.HOSPITALIZACION);
            assertThat(guardada.getCreatedBy())
                    .isEqualTo(HospitalizationObservationMother.VETERINARIO);
            assertThat(guardada.getDescription())
                    .isEqualTo(HospitalizationObservationMother.DESCRIPCION);
            assertThat(guardada.getId()).isNull();
        }

        @Test
        @DisplayName("devuelve el DTO de la observacion ya persistida")
        void devuelve_el_dto_de_la_observacion_ya_persistida() {
            referenciasResueltas();
            when(repository.save(any()))
                    .thenReturn(HospitalizationObservationMother.observacionValida());

            HospitalizationObservationDto dto = service
                    .execute(HospitalizationObservationMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(HospitalizationObservationMother.OBSERVATION_ID);
            assertThat(dto.description()).isEqualTo(HospitalizationObservationMother.DESCRIPCION);
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("hospitalizacion inexistente: no consulta el empleado ni persiste nada")
        void hospitalizacion_inexistente() {
            when(hospitalizationQueryPort
                    .findById(HospitalizationObservationMother.HOSPITALIZATION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationObservationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Hospitalization not found: "
                            + HospitalizationObservationMother.HOSPITALIZATION_ID);

            verifyNoInteractions(employeeQueryPort, repository);
        }

        @Test
        @DisplayName("empleado inexistente: no persiste nada")
        void empleado_inexistente() {
            when(hospitalizationQueryPort
                    .findById(HospitalizationObservationMother.HOSPITALIZATION_ID))
                    .thenReturn(Optional.of(HospitalizationObservationMother.HOSPITALIZACION));
            when(employeeQueryPort.findById(HospitalizationObservationMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(HospitalizationObservationMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Employee not found: " + HospitalizationObservationMother.EMPLOYEE_ID);

            verifyNoInteractions(repository);
        }
    }
}
