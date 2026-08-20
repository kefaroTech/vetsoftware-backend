package com.vetsoftware.app.medicationschedule.application.usecase;

import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.MEDICATION_ID;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.PRIMERA_TOMA;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.SCHEDULE_ID;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.orden;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.toma;
import static com.vetsoftware.app.medicationschedule.testsupport.MedicationScheduleMother.tomaAplicada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicationschedule.application.command.RescheduleMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.RescheduleResultDto;
import com.vetsoftware.app.medicationschedule.application.port.out.HospitalizationMedicationQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.CascadeSkipReason;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import com.vetsoftware.app.medicationschedule.domain.RescheduleMode;
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
@DisplayName("RescheduleMedicationScheduleService")
class RescheduleMedicationScheduleServiceTest {

    private static final LocalDateTime NUEVA_HORA = PRIMERA_TOMA.plusHours(2);
    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private MedicationScheduleRepository repository;
    @Mock
    private HospitalizationMedicationQueryPort medicationQueryPort;
    @InjectMocks
    private RescheduleMedicationScheduleService service;

    private static RescheduleMedicationScheduleCommand comando(RescheduleMode mode) {
        return new RescheduleMedicationScheduleCommand(SCHEDULE_ID, NUEVA_HORA, mode, COMPANY_ID);
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin nueva hora lanza excepcion y no toca ningun puerto")
        void sin_nueva_hora_lanza_excepcion() {
            assertThatThrownBy(
                    () -> service.execute(new RescheduleMedicationScheduleCommand(SCHEDULE_ID, null,
                            RescheduleMode.ONE, COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("newDateTime is required");

            verifyNoInteractions(repository, medicationQueryPort);
        }

        /**
         * El binder HTTP ya rechaza un modo desconocido, pero el caso de uso tambien lo
         * exige: por el camino SYSTEM el command no pasa por el request y un
         * {@code mode} nulo volveria a dejar que el alcance lo decidiera la omision.
         */
        @Test
        @DisplayName("sin modo lanza excepcion y no toca ningun puerto")
        void sin_modo_lanza_excepcion() {
            assertThatThrownBy(
                    () -> service.execute(new RescheduleMedicationScheduleCommand(SCHEDULE_ID,
                            NUEVA_HORA, null, COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mode is required");

            verifyNoInteractions(repository, medicationQueryPort);
        }

        @Test
        @DisplayName("una toma inexistente lanza excepcion y no escribe")
        void una_toma_inexistente_lanza_excepcion() {
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(RescheduleMode.ONE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Medication schedule not found: " + SCHEDULE_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(medicationQueryPort);
        }

        @Test
        @DisplayName("una toma que ya no aparece en el plan de su orden lanza excepcion")
        void una_toma_fuera_del_plan_lanza_excepcion() {
            MedicationSchedule probe = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(probe));
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("EVERY_8H", "FIXED", "DAYS", 3)));
            // El plan que devuelve findByHospitalizationMedicationId ya no trae la toma
            // encontrada por findById: inconsistencia que indexOfId tiene que frenar.
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID))
                    .thenReturn(List.of(toma(999L, PRIMERA_TOMA, AppliedStatus.PENDING)));

            assertThatThrownBy(() -> service.execute(comando(RescheduleMode.ONE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Medication schedule not found in plan: " + SCHEDULE_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Reprogramacion simple (mode=ONE)")
    class ModoUno {

        @Test
        @DisplayName("reprograma solo la toma pivote y no reporta desenlace de cascada")
        void reprograma_solo_la_toma_pivote() {
            MedicationSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("EVERY_8H", "FIXED", "DAYS", 3)));
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID)).thenReturn(List.of(pivote));

            RescheduleResultDto result = service.execute(comando(RescheduleMode.ONE));

            // No se pidio cascada: no hay nada que saltarse, asi que el motivo viaja
            // nulo igual que cuando la cascada si se aplica.
            assertThat(result.schedules()).hasSize(1);
            assertThat(result.cascadeApplied()).isFalse();
            assertThat(result.cascadeSkippedReason()).isNull();

            verify(medicationQueryPort, never()).findById(any());
            ArgumentCaptor<MedicationSchedule> captor = ArgumentCaptor
                    .forClass(MedicationSchedule.class);
            verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getCurrentDateTime()).isEqualTo(NUEVA_HORA);
            assertThat(captor.getValue().getRescheduled()).isTrue();
        }
    }

    /**
     * Estos casos son la red de #134. Pedir cascada no garantiza aplicarla, y las
     * tres salidas de escape devolvian antes un 200 con el plan intacto salvo el
     * pivote: indistinguible de una cascada que si corrio. Por eso ninguno se
     * conforma con contar {@code save} —eso es justo lo que certificaba el
     * defecto—: lo que queda afirmado es que el resultado <em>dice</em> que no se
     * aplico y por que.
     */
    @Nested
    @DisplayName("Reprogramacion en cascada (mode=CASCADE)")
    class ModoCascada {

        @Test
        @DisplayName("en pauta FIJA la cascada no se aplica y el resultado dice GUIDELINE_NOT_INTERVAL")
        void en_pauta_fija_la_cascada_se_salta_y_lo_reporta() {
            MedicationSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            MedicationSchedule siguiente = toma(501L, PRIMERA_TOMA.plusHours(8),
                    AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("EVERY_8H", "FIXED", "DAYS", 3)));
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID)).thenReturn(List.of(pivote, siguiente));

            RescheduleResultDto result = service.execute(comando(RescheduleMode.CASCADE));

            assertThat(result.cascadeApplied()).isFalse();
            assertThat(result.cascadeSkippedReason())
                    .isEqualTo(CascadeSkipReason.GUIDELINE_NOT_INTERVAL);
            assertThat(result.schedules()).hasSize(2);
            // Las horas de una pauta fija son de reloj: la siguiente no se mueve.
            assertThat(siguiente.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA.plusHours(8));

            ArgumentCaptor<MedicationSchedule> captor = ArgumentCaptor
                    .forClass(MedicationSchedule.class);
            verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(SCHEDULE_ID);
            assertThat(captor.getValue().getCurrentDateTime()).isEqualTo(NUEVA_HORA);
        }

        /**
         * Solo el camino SYSTEM llega aqui: con empresa, la orden ya quedo resuelta al
         * validar la propiedad, asi que «sin datos de la orden» seria un 404 antes de
         * tocar nada.
         */
        @Test
        @DisplayName("sin empresa y sin datos de la orden el resultado dice MEDICATION_ORDER_NOT_FOUND")
        void sin_datos_de_la_orden_la_cascada_se_salta_y_lo_reporta() {
            MedicationSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(repository.findByHospitalizationMedicationId(MEDICATION_ID))
                    .thenReturn(List.of(pivote));
            when(medicationQueryPort.findById(MEDICATION_ID)).thenReturn(Optional.empty());

            RescheduleResultDto result = service.execute(new RescheduleMedicationScheduleCommand(
                    SCHEDULE_ID, NUEVA_HORA, RescheduleMode.CASCADE, null));

            assertThat(result.cascadeApplied()).isFalse();
            assertThat(result.cascadeSkippedReason())
                    .isEqualTo(CascadeSkipReason.MEDICATION_ORDER_NOT_FOUND);

            ArgumentCaptor<MedicationSchedule> captor = ArgumentCaptor
                    .forClass(MedicationSchedule.class);
            verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(SCHEDULE_ID);
            assertThat(captor.getValue().getCurrentDateTime()).isEqualTo(NUEVA_HORA);
        }

        @Test
        @DisplayName("una frecuencia no discreta no tiene intervalo y el resultado dice FREQUENCY_NOT_DISCRETE")
        void frecuencia_no_discreta_la_cascada_se_salta_y_lo_reporta() {
            MedicationSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            MedicationSchedule siguiente = toma(501L, PRIMERA_TOMA.plusHours(8),
                    AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("CONTINUOUS", "INTERVAL", "DAYS", 3)));
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID)).thenReturn(List.of(pivote, siguiente));

            RescheduleResultDto result = service.execute(comando(RescheduleMode.CASCADE));

            assertThat(result.cascadeApplied()).isFalse();
            assertThat(result.cascadeSkippedReason())
                    .isEqualTo(CascadeSkipReason.FREQUENCY_NOT_DISCRETE);
            assertThat(siguiente.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA.plusHours(8));

            ArgumentCaptor<MedicationSchedule> captor = ArgumentCaptor
                    .forClass(MedicationSchedule.class);
            verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(SCHEDULE_ID);
        }

        @Test
        @DisplayName("en pauta INTERVALO recalcula las siguientes pendientes, salta las aplicadas y se reporta como aplicada")
        void en_pauta_intervalo_recalcula_y_salta_las_aplicadas() {
            MedicationSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            MedicationSchedule siguientePendiente = toma(501L, PRIMERA_TOMA.plusHours(8),
                    AppliedStatus.PENDING);
            MedicationSchedule intermediaAplicada = tomaAplicada(502L, PRIMERA_TOMA.plusHours(16),
                    PRIMERA_TOMA.plusHours(16));
            MedicationSchedule ultimaPendiente = toma(503L, PRIMERA_TOMA.plusHours(24),
                    AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("EVERY_8H", "INTERVAL", "DAYS", 3)));
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID))
                    .thenReturn(List.of(pivote, siguientePendiente, intermediaAplicada,
                            ultimaPendiente));

            RescheduleResultDto result = service.execute(comando(RescheduleMode.CASCADE));

            // La aplicada no se toca ni consume un turno del cursor: solo cuentan las
            // pendientes, por eso la ultima queda a +16h del pivote y no a +24h.
            assertThat(siguientePendiente.getCurrentDateTime()).isEqualTo(NUEVA_HORA.plusHours(8));
            assertThat(intermediaAplicada.getCurrentDateTime())
                    .isEqualTo(PRIMERA_TOMA.plusHours(16));
            assertThat(ultimaPendiente.getCurrentDateTime()).isEqualTo(NUEVA_HORA.plusHours(16));
            assertThat(result.cascadeApplied()).isTrue();
            assertThat(result.cascadeSkippedReason()).isNull();
            verify(repository, times(3)).save(any());
        }

        /**
         * El plan sale ordenado por hora vigente <em>despues</em> del movimiento: en el
         * orden de lectura el pivote adelantado apareceria donde ya no esta, y la
         * propia operacion desordenaria la respuesta.
         */
        @Test
        @DisplayName("el plan devuelto va ordenado por la hora vigente ya recalculada")
        void el_plan_devuelto_va_ordenado_por_hora_vigente() {
            MedicationSchedule pivote = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            MedicationSchedule siguiente = toma(501L, PRIMERA_TOMA.plusHours(8),
                    AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(pivote));
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(Optional.of(orden("EVERY_8H", "INTERVAL", "DAYS", 3)));
            when(repository.findByHospitalizationMedicationIdAndCompanyId(MEDICATION_ID,
                    COMPANY_ID)).thenReturn(List.of(pivote, siguiente));

            RescheduleResultDto result = service.execute(comando(RescheduleMode.CASCADE));

            assertThat(result.schedules()).extracting("currentDateTime").containsExactly(NUEVA_HORA,
                    NUEVA_HORA.plusHours(8));
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
        @DisplayName("la toma de un paciente de otra empresa no se reprograma")
        void la_toma_de_otra_empresa_no_se_reprograma() {
            MedicationSchedule ajena = toma(SCHEDULE_ID, PRIMERA_TOMA, AppliedStatus.PENDING);
            when(repository.findById(SCHEDULE_ID)).thenReturn(Optional.of(ajena));
            when(medicationQueryPort.findByIdAndCompanyId(MEDICATION_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new RescheduleMedicationScheduleCommand(SCHEDULE_ID,
                            NUEVA_HORA, RescheduleMode.CASCADE, OTRA_EMPRESA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Medication schedule not found: " + SCHEDULE_ID);

            verify(repository, never()).save(any());
            verify(repository, never()).findByHospitalizationMedicationId(any());
            verify(repository, never()).findByHospitalizationMedicationIdAndCompanyId(any(), any());
            assertThat(ajena.getCurrentDateTime()).isEqualTo(PRIMERA_TOMA);
        }
    }
}
