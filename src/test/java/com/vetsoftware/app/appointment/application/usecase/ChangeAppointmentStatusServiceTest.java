package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.command.ChangeAppointmentStatusCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics.Channel;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.domain.InvalidAppointmentTransitionException;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeAppointmentStatusService — avance del ciclo de vida de la cita")
class ChangeAppointmentStatusServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final Long ID = AppointmentMother.APPOINTMENT_ID;

    @Mock
    private AppointmentRepository repository;
    @Mock
    private AppointmentMetrics appointmentMetrics;
    @InjectMocks
    private ChangeAppointmentStatusService service;

    private static ChangeAppointmentStatusCommand comando(AppointmentStatus destino) {
        return new ChangeAppointmentStatusCommand(ID, destino, COMPANY);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"REQUESTED,CONFIRMED", "CONFIRMED,ARRIVED", "ARRIVED,IN_PROGRESS",
            "IN_PROGRESS,COMPLETED", "REQUESTED,NO_SHOW", "CONFIRMED,NO_SHOW"})
    @DisplayName("guarda la cita con el estado nuevo cuando la transicion es valida")
    void guarda_la_cita_con_el_estado_nuevo(AppointmentStatus origen, AppointmentStatus destino) {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conEstado(origen)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDto dto = service.execute(comando(destino));

        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getStatus()).isEqualTo(destino);
        assertThat(dto.status()).isEqualTo(destino);
    }

    @Test
    @DisplayName("registra la transicion en la telemetria por el canal de staff")
    void registra_la_transicion_en_la_telemetria() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(comando(AppointmentStatus.CONFIRMED));

        verify(appointmentMetrics).transitioned(AppointmentStatus.CONFIRMED, Channel.STAFF);
    }

    @Test
    @DisplayName("una cita de otra empresa es inexistente y no se escribe nada")
    void una_cita_de_otra_empresa_es_inexistente() {
        when(repository.findByIdAndCompanyId(ID, COMPANY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando(AppointmentStatus.CONFIRMED)))
                .isInstanceOf(AppointmentNotFoundException.class)
                .hasMessageContaining("Appointment not found: 55");

        verify(repository, never()).save(any());
        verifyNoInteractions(appointmentMetrics);
    }

    @Test
    @DisplayName("rechaza un salto de estado no permitido y no escribe ni mide")
    void rechaza_un_salto_de_estado_no_permitido() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));

        assertThatThrownBy(() -> service.execute(comando(AppointmentStatus.COMPLETED)))
                .isInstanceOf(InvalidAppointmentTransitionException.class)
                .hasMessageContaining("REQUESTED -> COMPLETED");

        verify(repository, never()).save(any());
        verifyNoInteractions(appointmentMetrics);
    }

    @Test
    @DisplayName("rechaza cambiar el estado de una cita ya completada")
    void rechaza_cambiar_el_estado_de_una_cita_completada() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conEstado(AppointmentStatus.COMPLETED)));

        assertThatThrownBy(() -> service.execute(comando(AppointmentStatus.IN_PROGRESS)))
                .isInstanceOf(InvalidAppointmentTransitionException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("exige un estado destino: un comando sin estado no llega al repositorio")
    void exige_un_estado_destino() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));

        assertThatThrownBy(() -> service.execute(comando(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status is required");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("cancelar por esta via conserva el motivo ya registrado en la cita")
    void cancelar_por_esta_via_conserva_el_motivo_registrado() {
        Appointment cita = new Appointment(ID, AppointmentMother.INICIO,
                AppointmentType.CONSULTATION, AppointmentStatus.CONFIRMED, null, "El dueno aviso",
                AppointmentMother.FIRULAIS, null, null, null, null, AppointmentMother.VETERINARIA,
                AppointmentMother.CLINICA, AppointmentMother.PRINCIPAL, 1L, true,
                AppointmentMother.CREADA);
        when(repository.findByIdAndCompanyId(ID, COMPANY)).thenReturn(Optional.of(cita));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDto dto = service.execute(comando(AppointmentStatus.CANCELLED));

        assertThat(dto.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(dto.cancellationReason()).isEqualTo("El dueno aviso");
    }
}
