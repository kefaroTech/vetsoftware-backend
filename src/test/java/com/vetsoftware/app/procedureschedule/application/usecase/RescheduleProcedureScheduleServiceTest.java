package com.vetsoftware.app.procedureschedule.application.usecase;

import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.PROCEDURE_ID;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.SCHEDULE_ID;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.orden;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.toma;
import static com.vetsoftware.app.procedureschedule.testsupport.ProcedureScheduleMother.tomaAplicada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.procedureschedule.application.command.RescheduleProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.out.HospitalizationProcedureQueryPort;
import com.vetsoftware.app.procedureschedule.application.port.out.ProcedureScheduleRepository;
import com.vetsoftware.app.procedureschedule.domain.AppliedStatus;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import java.time.LocalDateTime;
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
@DisplayName("RescheduleProcedureScheduleService")
class RescheduleProcedureScheduleServiceTest {

    private static final LocalDateTime NUEVA_HORA = PRIMERA_TOMA.plusHours(2);
    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private ProcedureScheduleRepository repository;
    @Mock
    private HospitalizationProcedureQueryPort procedureQueryPort;
    @InjectMocks
    private RescheduleProcedureScheduleService service;

    private static RescheduleProcedureScheduleCommand comando(String mode) {
        return new RescheduleProcedureScheduleCommand(SCHEDULE_ID, NUEVA_HORA, mode, COMPANY_ID);
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin nueva hora lanza excepcion y no toca ningun puerto")
        void sin_nueva_hora_lanza_excepcion() {
            assertThatThrownBy(() -> service.execute(
                    new RescheduleProcedureScheduleCommand(SCHEDULE_ID, null, "one", COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("newDateTime is required");

            verifyNoInteractions(repository, procedureQueryPort);
        }

        @Test
        @DisplayName("una toma inexistente lanza excepcion y no escribe")
        void una_toma_inexistente_lanza_excepcion() {
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando("one")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Procedure schedule not found: " + SCHEDULE_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(procedureQueryPort);
        }

        @Test
        @DisplayName("una toma que ya no aparece en el plan de su orden lanza excepcion")
        void una_toma_fuera_del_plan_lanza_excepcion() {
            ProcedureSchedule probe = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(probe));
            when(procedureQueryPort.findByIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("EVERY_8H", "FIXED", "DAYS", 3)));
            // El plan que devuelve findByHospitalizationProcedureId ya no trae la toma
            // encontrada por findById: inconsistencia que indexOfId tiene que frenar.
            when(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(List.of(toma(999L, PRIMERA_TOMA, AppliedStatus.PENDING)));

            assertThatThrownBy(() -> service.execute(comando("one")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Procedure schedule not found in plan: " + SCHEDULE_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Reprogramacion simple (mode=one)")
    class ModoUno {

        @Test
        @DisplayName("reprograma solo la toma pivote y no vuelve a resolver la orden sin acotar")
        void reprograma_solo_la_toma_pivote() {
            ProcedureSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(procedureQueryPort.findByIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("EVERY_8H", "FIXED", "DAYS", 3)));
            when(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(List.of(pivote));

            List<ProcedureScheduleDto> result = service.execute(comando("one"));

            verify(procedureQueryPort, never()).findById(any());
            ArgumentCaptor<ProcedureSchedule> captor = ArgumentCaptor
                    .forClass(ProcedureSchedule.class);
            verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getCurrentDateTime()).isEqualTo(NUEVA_HORA);
            assertThat(captor.getValue().getRescheduled()).isTrue();
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Reprogramacion en cascada (mode=cascade)")
    class ModoCascada {

        @Test
        @DisplayName("en pauta FIJA la cascada no recalcula nada mas que el pivote")
        void en_pauta_fija_no_recalcula_nada_mas() {
            ProcedureSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            ProcedureSchedule siguiente = toma(501L, PRIMERA_TOMA.plusHours(8),
                    AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(procedureQueryPort.findByIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("EVERY_8H", "FIXED", "DAYS", 3)));
            when(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(List.of(pivote, siguiente));

            service.execute(comando("cascade"));

            verify(repository, times(1)).save(any());
            assertThat(siguiente.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA.plusHours(8));
        }

        /**
         * Solo el camino SYSTEM llega aqui: con empresa, la orden ya quedo resuelta al
         * validar la propiedad, asi que «sin datos de la orden» seria un 404 antes de
         * tocar nada.
         */
        @Test
        @DisplayName("sin empresa y sin datos de la orden la cascada tampoco recalcula")
        void sin_datos_de_la_orden_no_recalcula() {
            ProcedureSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(repository.findByHospitalizationProcedureId(PROCEDURE_ID))
                    .thenReturn(List.of(pivote));
            when(procedureQueryPort.findById(PROCEDURE_ID)).thenReturn(Optional.empty());

            service.execute(new RescheduleProcedureScheduleCommand(SCHEDULE_ID, NUEVA_HORA,
                    "cascade", null));

            verify(repository, times(1)).save(any());
        }

        @Test
        @DisplayName("una frecuencia no discreta no tiene intervalo que propagar")
        void frecuencia_no_discreta_no_recalcula() {
            ProcedureSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            ProcedureSchedule siguiente = toma(501L, PRIMERA_TOMA.plusHours(8),
                    AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(procedureQueryPort.findByIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("CONTINUOUS", "INTERVAL", "DAYS", 3)));
            when(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(List.of(pivote, siguiente));

            service.execute(comando("cascade"));

            verify(repository, times(1)).save(any());
            assertThat(siguiente.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA.plusHours(8));
        }

        @Test
        @DisplayName("en pauta INTERVALO recalcula las siguientes pendientes y salta las aplicadas")
        void en_pauta_intervalo_recalcula_y_salta_las_aplicadas() {
            ProcedureSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            ProcedureSchedule siguientePendiente = toma(501L, PRIMERA_TOMA.plusHours(8),
                    AppliedStatus.PENDING);
            ProcedureSchedule intermediaAplicada = tomaAplicada(502L, PRIMERA_TOMA.plusHours(16),
                    PRIMERA_TOMA.plusHours(16));
            ProcedureSchedule ultimaPendiente = toma(503L, PRIMERA_TOMA.plusHours(24),
                    AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(procedureQueryPort.findByIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("EVERY_8H", "INTERVAL", "DAYS", 3)));
            when(repository.findByHospitalizationProcedureIdAndCompanyId(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(List.of(pivote, siguientePendiente, intermediaAplicada,
                            ultimaPendiente));

            service.execute(comando("cascade"));

            // La aplicada no se toca ni consume un turno del cursor: solo cuentan las
            // pendientes, por eso la ultima queda a +16h del pivote y no a +24h.
            assertThat(siguientePendiente.getCurrentDateTime()).isEqualTo(NUEVA_HORA.plusHours(8));
            assertThat(intermediaAplicada.getCurrentDateTime())
                    .isEqualTo(PRIMERA_TOMA.plusHours(16));
            assertThat(ultimaPendiente.getCurrentDateTime()).isEqualTo(NUEVA_HORA.plusHours(16));
            verify(repository, times(3)).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * En cascada esto no movia una fila sino toda la pauta pendiente de un paciente
         * ajeno, asi que el corte tiene que ocurrir antes de leer el plan y antes del
         * primer save.
         */
        @Test
        @DisplayName("la ejecucion de un paciente de otra empresa no se reprograma")
        void la_ejecucion_de_otra_empresa_no_se_reprograma() {
            ProcedureSchedule ajena = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(ajena));
            when(procedureQueryPort.findByIdAndCompanyId(PROCEDURE_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new RescheduleProcedureScheduleCommand(SCHEDULE_ID,
                            NUEVA_HORA, "cascade", OTRA_EMPRESA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Procedure schedule not found: " + SCHEDULE_ID);

            verify(repository, never()).save(any());
            verify(repository, never()).findByHospitalizationProcedureId(any());
            verify(repository, never()).findByHospitalizationProcedureIdAndCompanyId(any(), any());
            assertThat(ajena.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA);
        }
    }
}
