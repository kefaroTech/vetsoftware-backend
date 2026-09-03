package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentConfirmationEmailSender;
import com.vetsoftware.app.appointment.application.port.out.AppointmentDurationPolicyPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.BranchQueryPort;
import com.vetsoftware.app.appointment.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.domain.BranchRef;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Resolución de sede al crear una cita (multi-sucursal, Fase B). Reglas: si el
 * request trae branchId, la sede debe pertenecer a la empresa y estar ACTIVA;
 * si no, se usa la sede ACTIVA por defecto. Una sede desactivada se rechaza con
 * un error DISTINTO al de una sede inexistente. Ningún camino de error debe
 * escribir.
 */
@ExtendWith(MockitoExtension.class)
class CreateAppointmentServiceBranchTest {

    @Mock
    private AppointmentRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private OwnerQueryPort ownerQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private AppointmentConfirmationEmailSender confirmationEmailSender;
    @Mock
    private AppointmentMetrics appointmentMetrics;
    @Mock
    private AppointmentDurationPolicyPort durationPolicyPort;
    @InjectMocks
    private CreateAppointmentService service;

    private static final long COMPANY = 9L;
    private static final int DURACION_POR_DEFECTO = 30;
    private final EmployeeRef employee = new EmployeeRef(4L, "Dra. Vet");
    private final BranchRef requested = new BranchRef(11L, "Sede Norte", "NORTE");
    private final BranchRef principal = new BranchRef(1L, "Principal", "PRINCIPAL");
    private final LocalDateTime startAt = LocalDateTime.of(2026, 8, 1, 9, 0);

    private CreateAppointmentCommand command(Long branchId) {
        return new CreateAppointmentCommand(startAt, null, AppointmentType.CONSULTATION, 4L, null,
                null, "Walk-in", null, null, null, branchId, COMPANY, false, Set.of(1L, 11L));
    }

    private ArgumentCaptor<Appointment> stubSaveSinSolapes() {
        when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DURACION_POR_DEFECTO);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findOverlapping(eq(COMPANY), eq(4L), any(), any(), eq(DURACION_POR_DEFECTO),
                isNull())).thenReturn(List.of());
        return ArgumentCaptor.forClass(Appointment.class);
    }

    @Test
    void usa_la_sede_solicitada_cuando_es_valida_y_activa() {
        when(employeeQueryPort.findByIdAndCompanyId(4L, COMPANY)).thenReturn(Optional.of(employee));
        when(branchQueryPort.findActiveByIdAndCompanyId(11L, COMPANY))
                .thenReturn(Optional.of(requested));
        ArgumentCaptor<Appointment> captor = stubSaveSinSolapes();

        AppointmentDto dto = service.execute(command(11L));

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBranch()).isEqualTo(requested);
        assertThat(dto.branch().id()).isEqualTo(11L);
        verify(branchQueryPort, never()).findDefaultActiveByCompanyId(any());
    }

    @Test
    void rechaza_una_sede_solicitada_INACTIVA_con_error_distinto_y_no_escribe() {
        when(employeeQueryPort.findByIdAndCompanyId(4L, COMPANY)).thenReturn(Optional.of(employee));
        when(branchQueryPort.findActiveByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.empty());
        when(branchQueryPort.existsByIdAndCompanyId(11L, COMPANY)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(command(11L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Branch is not active").hasMessageContaining("11");

        verify(repository, never()).save(any());
    }

    @Test
    void rechaza_una_sede_inexistente_o_ajena_con_error_de_not_found_y_no_escribe() {
        when(employeeQueryPort.findByIdAndCompanyId(4L, COMPANY)).thenReturn(Optional.of(employee));
        when(branchQueryPort.findActiveByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.empty());
        when(branchQueryPort.existsByIdAndCompanyId(11L, COMPANY)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(command(11L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Branch not found");

        verify(repository, never()).save(any());
    }

    @Test
    void cae_a_la_sede_activa_por_defecto_cuando_no_viene_branchId() {
        when(employeeQueryPort.findByIdAndCompanyId(4L, COMPANY)).thenReturn(Optional.of(employee));
        when(branchQueryPort.findDefaultActiveByCompanyId(COMPANY))
                .thenReturn(Optional.of(principal));
        ArgumentCaptor<Appointment> captor = stubSaveSinSolapes();

        AppointmentDto dto = service.execute(command(null));

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBranch()).isEqualTo(principal);
        assertThat(dto.branch().code()).isEqualTo("PRINCIPAL");
        verify(branchQueryPort, never()).findActiveByIdAndCompanyId(any(), any());
    }

    @Test
    void falla_si_la_empresa_no_tiene_ninguna_sede_activa_y_no_escribe() {
        when(employeeQueryPort.findByIdAndCompanyId(4L, COMPANY)).thenReturn(Optional.of(employee));
        when(branchQueryPort.findDefaultActiveByCompanyId(COMPANY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(command(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company has no active branch");

        verify(repository, never()).save(any());
    }
}
