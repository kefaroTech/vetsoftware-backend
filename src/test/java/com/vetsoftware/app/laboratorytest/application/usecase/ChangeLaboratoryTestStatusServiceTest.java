package com.vetsoftware.app.laboratorytest.application.usecase;

import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.BACTERIOLOGA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.PROCESADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytest.application.command.ChangeLaboratoryTestStatusCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeLaboratoryTestStatusService")
class ChangeLaboratoryTestStatusServiceTest {

    private static final Long ID = LaboratoryTestMother.ID;
    private static final Long EMPLOYEE_ID = 3L;

    @Mock
    private LaboratoryTestRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;

    @InjectMocks
    private ChangeLaboratoryTestStatusService service;

    @Captor
    private ArgumentCaptor<LaboratoryTest> guardada;

    private static ChangeLaboratoryTestStatusCommand comando(String status, Long processedById) {
        return new ChangeLaboratoryTestStatusCommand(ID, status, processedById);
    }

    private void muestraPendiente() {
        when(repository.findById(ID))
                .thenReturn(Optional.of(LaboratoryTestMother.pendienteDeToma()));
    }

    private void devuelveLoQueGuarda() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Transiciones sin firma")
    class SinFirma {

        @ParameterizedTest
        @EnumSource(value = LaboratoryTestStatus.class, names = "PENDING_VALIDATION", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("mueve la muestra al estado pedido sin consultar al empleado")
        void mueve_la_muestra_sin_consultar_al_empleado(LaboratoryTestStatus destino) {
            muestraPendiente();
            devuelveLoQueGuarda();

            service.execute(comando(destino.name(), EMPLOYEE_ID));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus()).isEqualTo(destino);
            assertThat(guardada.getValue().getProcessedBy()).isNull();
            verifyNoInteractions(employeeQueryPort);
        }

        @Test
        @DisplayName("acepta el estado en minusculas como lo manda el front")
        void acepta_el_estado_en_minusculas() {
            muestraPendiente();
            devuelveLoQueGuarda();

            LaboratoryTestDto dto = service.execute(comando("in_progress", null));

            assertThat(dto.status()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("pasar a validacion sin empleado no firma la muestra")
        void pasar_a_validacion_sin_empleado_no_firma() {
            muestraPendiente();
            devuelveLoQueGuarda();

            service.execute(comando("PENDING_VALIDATION", null));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus())
                    .isEqualTo(LaboratoryTestStatus.PENDING_VALIDATION);
            assertThat(guardada.getValue().getProcessedBy()).isNull();
            assertThat(guardada.getValue().getProcessedDate()).isNull();
            verifyNoInteractions(employeeQueryPort);
        }

        @Test
        @DisplayName("una transicion sin firma conserva la firma que ya tuviera la muestra")
        void una_transicion_sin_firma_conserva_la_firma_previa() {
            when(repository.findById(ID)).thenReturn(Optional.of(LaboratoryTestMother.validada()));
            devuelveLoQueGuarda();

            service.execute(comando("CANCELLED", EMPLOYEE_ID));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(guardada.getValue().getProcessedDate()).isEqualTo(PROCESADO);
        }
    }

    @Nested
    @DisplayName("Transicion a validacion — la que firma")
    class ConFirma {

        @Test
        @DisplayName("registra quien valido y el instante de la validacion")
        void registra_quien_valido_y_cuando() {
            muestraPendiente();
            when(employeeQueryPort.findById(EMPLOYEE_ID)).thenReturn(Optional.of(BACTERIOLOGA));
            devuelveLoQueGuarda();

            service.execute(comando("PENDING_VALIDATION", EMPLOYEE_ID));

            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getStatus())
                    .isEqualTo(LaboratoryTestStatus.PENDING_VALIDATION);
            assertThat(guardada.getValue().getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(guardada.getValue().getProcessedDate()).isCloseTo(LocalDateTime.now(),
                    within(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("un empleado inexistente no cambia el estado ni escribe")
        void un_empleado_inexistente_no_cambia_el_estado() {
            muestraPendiente();
            when(employeeQueryPort.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando("PENDING_VALIDATION", EMPLOYEE_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: 3");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Rechazos")
    class Rechazos {

        @Test
        @DisplayName("una muestra inexistente no consulta al empleado ni guarda")
        void una_muestra_inexistente_no_consulta_ni_guarda() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando("COMPLETED", EMPLOYEE_ID)))
                    .isInstanceOf(LaboratoryTestNotFoundException.class)
                    .hasMessageContaining("LaboratoryTest not found: 42");

            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort);
        }

        @Test
        @DisplayName("un estado desconocido no llega a la base")
        void un_estado_desconocido_no_llega_a_la_base() {
            muestraPendiente();

            assertThatThrownBy(() -> service.execute(comando("ARCHIVED", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No enum constant");

            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort);
        }
    }
}
