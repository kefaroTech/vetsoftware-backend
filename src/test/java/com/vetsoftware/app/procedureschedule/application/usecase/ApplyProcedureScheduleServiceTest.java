package com.vetsoftware.app.procedureschedule.application.usecase;

import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.PROCEDURE_ID;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.SCHEDULE_ID;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.cada8hDurante3Dias;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.tomaPendiente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.procedureschedule.application.command.ApplyProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.out.HospitalizationProcedureQueryPort;
import com.vetsoftware.app.procedureschedule.application.port.out.ProcedureScheduleRepository;
import com.vetsoftware.app.procedureschedule.domain.AppliedStatus;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplyProcedureScheduleService")
class ApplyProcedureScheduleServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private ProcedureScheduleRepository repository;
    @Mock
    private HospitalizationProcedureQueryPort procedureQueryPort;
    @InjectMocks
    private ApplyProcedureScheduleService service;

    @Nested
    @DisplayName("Aplicacion")
    class Aplicacion {

        @Test
        @DisplayName("marca la toma como aplicada y devuelve el plan actualizado de la orden")
        void marca_la_toma_como_aplicada_y_devuelve_el_plan() {
            ProcedureSchedule toma = tomaPendiente();
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(toma));
            when(procedureQueryPort.findByIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cada8hDurante3Dias()));
            when(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(List.of(toma));

            List<ProcedureScheduleDto> result = service
                    .execute(new ApplyProcedureScheduleCommand(SCHEDULE_ID, COMPANY_ID));

            ArgumentCaptor<ProcedureSchedule> captor = ArgumentCaptor
                    .forClass(ProcedureSchedule.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAppliedStatus()).isEqualTo(AppliedStatus.APPLIED);
            assertThat(captor.getValue().getRealDateTime()).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo(SCHEDULE_ID);
        }

        @Test
        @DisplayName("sin empresa (camino SYSTEM) aplica sin consultar la orden")
        void sin_empresa_aplica_sin_consultar_la_orden() {
            ProcedureSchedule toma = tomaPendiente();
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(toma));
            when(repository.findByHospitalizationProcedureId(PROCEDURE_ID))
                    .thenReturn(List.of(toma));

            service.execute(new ApplyProcedureScheduleCommand(SCHEDULE_ID, null));

            verifyNoInteractions(procedureQueryPort);
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una toma inexistente lanza excepcion y no escribe nada")
        void una_toma_inexistente_lanza_excepcion_y_no_escribe() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new ApplyProcedureScheduleCommand(999L, COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Procedure schedule not found: 999");

            verify(repository, never()).save(any());
            verifyNoInteractions(procedureQueryPort);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * La ejecucion no tiene empresa propia: la unica prueba de propiedad es que la
         * orden de procedimiento padre pertenezca al tenant. Si no, el servicio tiene
         * que frenar ANTES de marcar nada — una ejecucion aplicada falsea la hoja de un
         * paciente ajeno y no se puede deshacer desde la UI.
         */
        @Test
        @DisplayName("la ejecucion de un paciente de otra empresa no se marca como aplicada")
        void la_ejecucion_de_otra_empresa_no_se_marca_como_aplicada() {
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(tomaPendiente()));
            when(procedureQueryPort.findByIdAndCompanyId(PROCEDURE_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new ApplyProcedureScheduleCommand(SCHEDULE_ID, OTRA_EMPRESA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Procedure schedule not found: " + SCHEDULE_ID);

            verify(repository, never()).save(any());
            verify(repository, never()).findByHospitalizationProcedureId(any());
            verify(repository, never()).findByHospitalizationProcedureIdAndCompanyId(any(), any());
        }
    }
}
