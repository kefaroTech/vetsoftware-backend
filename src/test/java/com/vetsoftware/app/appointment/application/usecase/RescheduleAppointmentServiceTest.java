package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RescheduleAppointmentService — mover una cita de hora o de veterinario")
class RescheduleAppointmentServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final Long ID = AppointmentMother.APPOINTMENT_ID;
    private static final Long OTRO_EMPLEADO = 5L;

    @Mock
    private AppointmentRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @InjectMocks
    private RescheduleAppointmentService service;

    private static RescheduleAppointmentCommand comando() {
        return new RescheduleAppointmentCommand(ID, AppointmentMother.NUEVO_INICIO, OTRO_EMPLEADO,
                COMPANY);
    }

    @Test
    @DisplayName("guarda la cita con la hora y el veterinario nuevos, sin tocar el estado")
    void guarda_la_cita_con_la_hora_y_el_veterinario_nuevos() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conEstado(AppointmentStatus.CONFIRMED)));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findClashingIds(eq(COMPANY), eq(OTRO_EMPLEADO), any(), any()))
                .thenReturn(List.of());

        service.execute(comando());

        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        Appointment cita = guardada.getValue();
        assertThat(cita.getStartAt()).isEqualTo(AppointmentMother.NUEVO_INICIO);
        assertThat(cita.getEmployee()).isEqualTo(AppointmentMother.OTRO_VETERINARIO);
        assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("devuelve como aviso las citas del veterinario nuevo a esa hora")
    void devuelve_como_aviso_las_citas_del_veterinario_nuevo() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findClashingIds(eq(COMPANY), eq(OTRO_EMPLEADO),
                eq(AppointmentMother.NUEVO_INICIO), eq(ID))).thenReturn(List.of(90L, 91L));

        AppointmentDto dto = service.execute(comando());

        assertThat(dto.overlappingAppointmentIds()).containsExactly(90L, 91L);
    }

    @Test
    @DisplayName("una cita de otra empresa es inexistente y no se escribe nada")
    void una_cita_de_otra_empresa_es_inexistente() {
        when(repository.findByIdAndCompanyId(ID, COMPANY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(AppointmentNotFoundException.class)
                .hasMessageContaining("Appointment not found: 55");

        verify(repository, never()).save(any());
        verifyNoInteractions(employeeQueryPort);
    }

    @Test
    @DisplayName("no reprograma sobre un veterinario de otra empresa")
    void no_reprograma_sobre_un_veterinario_de_otra_empresa() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee not found: 5");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("propaga la invariante del dominio si la hora nueva viene vacia")
    void propaga_la_invariante_si_la_hora_nueva_viene_vacia() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));

        assertThatThrownBy(() -> service
                .execute(new RescheduleAppointmentCommand(ID, null, OTRO_EMPLEADO, COMPANY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startAt is required");

        verify(repository, never()).save(any());
    }
}
