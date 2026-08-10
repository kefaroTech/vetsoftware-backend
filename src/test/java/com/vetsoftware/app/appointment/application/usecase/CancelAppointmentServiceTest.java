package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.command.CancelAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics.Channel;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.InvalidAppointmentTransitionException;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelAppointmentService — cancelacion de una cita")
class CancelAppointmentServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final Long ID = AppointmentMother.APPOINTMENT_ID;

    @Mock
    private AppointmentRepository repository;
    @Mock
    private AppointmentMetrics appointmentMetrics;
    @InjectMocks
    private CancelAppointmentService service;

    private static CancelAppointmentCommand comando(String motivo) {
        return new CancelAppointmentCommand(ID, motivo, COMPANY);
    }

    @Test
    @DisplayName("guarda la cita cancelada con el motivo recibido")
    void guarda_la_cita_cancelada_con_el_motivo() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDto dto = service.execute(comando("El dueno no puede asistir"));

        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(guardada.getValue().getCancellationReason())
                .isEqualTo("El dueno no puede asistir");
        assertThat(dto.cancellationReason()).isEqualTo("El dueno no puede asistir");
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"REQUESTED", "CONFIRMED", "ARRIVED",
            "IN_PROGRESS"})
    @DisplayName("cancela desde cualquier estado vivo de la agenda")
    void cancela_desde_cualquier_estado_vivo(AppointmentStatus origen) {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conEstado(origen)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.execute(comando("motivo")).status())
                .isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("un motivo nulo deja la cita cancelada sin motivo registrado")
    void un_motivo_nulo_deja_la_cita_sin_motivo() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDto dto = service.execute(comando(null));

        assertThat(dto.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(dto.cancellationReason()).isNull();
    }

    @Test
    @DisplayName("registra la cancelacion en la telemetria por el canal de staff")
    void registra_la_cancelacion_en_la_telemetria() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(comando("motivo"));

        verify(appointmentMetrics).transitioned(AppointmentStatus.CANCELLED, Channel.STAFF);
    }

    @Test
    @DisplayName("una cita de otra empresa es inexistente y no se escribe nada")
    void una_cita_de_otra_empresa_es_inexistente() {
        when(repository.findByIdAndCompanyId(ID, COMPANY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando("motivo")))
                .isInstanceOf(AppointmentNotFoundException.class)
                .hasMessageContaining("Appointment not found: 55");

        verify(repository, never()).save(any());
        verifyNoInteractions(appointmentMetrics);
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "NO_SHOW", "CANCELLED"})
    @DisplayName("no cancela una cita ya terminal y no escribe ni mide")
    void no_cancela_una_cita_ya_terminal(AppointmentStatus terminal) {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conEstado(terminal)));

        assertThatThrownBy(() -> service.execute(comando("motivo")))
                .isInstanceOf(InvalidAppointmentTransitionException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(appointmentMetrics);
    }

    @Test
    @DisplayName("rechaza un motivo de mas de 300 caracteres y no escribe")
    void rechaza_un_motivo_demasiado_largo() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));

        assertThatThrownBy(() -> service.execute(comando("m".repeat(301))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancellationReason must be 300 chars or less");

        verify(repository, never()).save(any());
        verifyNoInteractions(appointmentMetrics);
    }
}
