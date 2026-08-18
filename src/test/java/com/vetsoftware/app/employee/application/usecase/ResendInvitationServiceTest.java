package com.vetsoftware.app.employee.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.application.command.ResendInvitationCommand;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.out.EmployeeInvitationEmailSender;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.application.port.out.EmployeeRolesQueryPort;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.employee.domain.EmployeeNotFoundException;
import com.vetsoftware.app.employee.domain.EmployeeStatus;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reenvío de la invitación: asigna una nueva contraseña provisional a un
 * empleado que sigue INVITED. Se fija el aislamiento por empresa (cross tenant
 * -> 404 sin filtrar datos, igual que "no encontrado") y que solo se reenvía a
 * quien nunca inició sesión.
 */
@ExtendWith(MockitoExtension.class)
class ResendInvitationServiceTest {

    @Mock
    private EmployeeRepository repository;
    @Mock
    private EmployeeRolesQueryPort rolesQueryPort;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private EmployeeInvitationEmailSender invitationEmailSender;
    @InjectMocks
    private ResendInvitationService service;

    private static ResendInvitationCommand comando() {
        return new ResendInvitationCommand(EmployeeMother.EMPLOYEE_ID, "Nueva123*",
                EmployeeMother.COMPANY_ID);
    }

    @Nested
    class ReenvioCorrecto {

        @Test
        @DisplayName("hashea la nueva clave temporal y la guarda en el empleado")
        void hashea_la_nueva_clave_temporal() {
            when(repository.findByIdAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    EmployeeMother.COMPANY_ID))
                    .thenReturn(java.util.Optional.of(EmployeeMother.invitado()));
            when(passwordHasher.hash("Nueva123*")).thenReturn("$2a$10$nuevo");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(rolesQueryPort.findRolesByEmployeeIds(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(Map.of());

            service.execute(comando());

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getHashPassword()).isEqualTo("$2a$10$nuevo");
            assertThat(captor.getValue().getStatus()).isEqualTo(EmployeeStatus.INVITED);
            assertThat(captor.getValue().isMustChangePassword()).isTrue();
        }

        @Test
        @DisplayName("envia el correo con los nombres de los roles vigentes separados por coma")
        void envia_el_correo_con_los_roles_vigentes() {
            Employee invitado = EmployeeMother.invitado();
            when(repository.findByIdAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    EmployeeMother.COMPANY_ID)).thenReturn(java.util.Optional.of(invitado));
            when(passwordHasher.hash(anyString())).thenReturn("$2a$10$nuevo");
            when(repository.save(any())).thenReturn(invitado);
            when(rolesQueryPort.findRolesByEmployeeIds(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(Map.of(EmployeeMother.EMPLOYEE_ID,
                            List.of(EmployeeMother.VETERINARIO, EmployeeMother.CAJERO)));

            service.execute(comando());

            verify(invitationEmailSender).send(invitado.getEmail(), invitado.getName(),
                    invitado.getCompany().name(), invitado.getEmployeeCode(), "Nueva123*",
                    "Veterinario, Cajero");
        }

        @Test
        @DisplayName("devuelve el dto del empleado con sus roles")
        void devuelve_el_dto_del_empleado_con_sus_roles() {
            Employee invitado = EmployeeMother.invitado();
            when(repository.findByIdAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    EmployeeMother.COMPANY_ID)).thenReturn(java.util.Optional.of(invitado));
            when(passwordHasher.hash(anyString())).thenReturn("$2a$10$nuevo");
            when(repository.save(any())).thenReturn(invitado);
            when(rolesQueryPort.findRolesByEmployeeIds(List.of(EmployeeMother.EMPLOYEE_ID)))
                    .thenReturn(Map.of(EmployeeMother.EMPLOYEE_ID,
                            List.of(EmployeeMother.VETERINARIO)));

            EmployeeDto dto = service.execute(comando());

            assertThat(dto.id()).isEqualTo(EmployeeMother.EMPLOYEE_ID);
            assertThat(dto.roles()).extracting("code").containsExactly("VET");
        }
    }

    @Nested
    class Rechazos {

        @Test
        @DisplayName("un empleado inexistente no escribe nada")
        void un_empleado_inexistente_no_escribe_nada() {
            when(repository.findByIdAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    EmployeeMother.COMPANY_ID)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(passwordHasher, invitationEmailSender, rolesQueryPort);
        }

        @Test
        @DisplayName("un empleado de otra empresa se trata como inexistente, sin filtrar datos")
        void un_empleado_de_otra_empresa_se_trata_como_inexistente() {
            // El filtro por empresa vive AHORA en la consulta, no en un if posterior: el
            // empleado del otro tenant no llega a cargarse.
            when(repository.findByIdAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    EmployeeMother.COMPANY_ID)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(EmployeeNotFoundException.class);

            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
            verifyNoInteractions(passwordHasher, invitationEmailSender);
        }

        @Test
        @DisplayName("un empleado que ya esta activo no se puede reinvitar")
        void un_empleado_ya_activo_no_se_puede_reinvitar() {
            when(repository.findByIdAndCompanyId(EmployeeMother.EMPLOYEE_ID,
                    EmployeeMother.COMPANY_ID))
                    .thenReturn(java.util.Optional.of(EmployeeMother.activo()));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("invitado");

            verify(repository, never()).save(any());
            verifyNoInteractions(passwordHasher, invitationEmailSender);
        }
    }
}
