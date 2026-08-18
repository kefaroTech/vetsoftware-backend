package com.vetsoftware.app.employeerole.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeerole.application.command.UpdateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.application.port.out.PermissionCachePort;
import com.vetsoftware.app.employeerole.application.port.out.RoleQueryPort;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
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
@DisplayName("UpdateEmployeeRoleService")
class UpdateEmployeeRoleServiceTest {

    @Mock
    private EmployeeRoleRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private RoleQueryPort roleQueryPort;
    @Mock
    private PermissionCachePort permissionCachePort;

    @InjectMocks
    private UpdateEmployeeRoleService service;

    @Captor
    private ArgumentCaptor<EmployeeRole> employeeRoleCaptor;

    @Nested
    @DisplayName("actualizacion valida")
    class Actualizacion {

        @Test
        @DisplayName("reasignar a otro empleado evita la cache del anterior titular y del nuevo")
        void reasignar_a_otro_empleado_evita_ambas_caches() {
            when(repository.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.habilitado()));
            when(employeeQueryPort.findById(EmployeeRoleMother.OTRO_EMPLEADO.id()))
                    .thenReturn(Optional.of(EmployeeRoleMother.OTRO_EMPLEADO));
            when(roleQueryPort.findById(EmployeeRoleMother.ROL_RECEPCION.id()))
                    .thenReturn(Optional.of(EmployeeRoleMother.ROL_RECEPCION));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            EmployeeRoleDto dto = service.execute(EmployeeRoleMother.comandoActualizar());

            verify(repository).save(employeeRoleCaptor.capture());
            assertThat(employeeRoleCaptor.getValue().getEmployee())
                    .isEqualTo(EmployeeRoleMother.OTRO_EMPLEADO);
            assertThat(employeeRoleCaptor.getValue().getRole())
                    .isEqualTo(EmployeeRoleMother.ROL_RECEPCION);
            assertThat(dto.employee().id()).isEqualTo(EmployeeRoleMother.OTRO_EMPLEADO.id());
            verify(permissionCachePort).evictByEmployeeId(EmployeeRoleMother.EMPLEADO.id());
            verify(permissionCachePort).evictByEmployeeId(EmployeeRoleMother.OTRO_EMPLEADO.id());
        }

        @Test
        @DisplayName("cambiar solo el rol, con el mismo empleado, evita la cache una unica vez")
        void mismo_empleado_evita_la_cache_una_sola_vez() {
            when(repository.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.habilitado()));
            when(employeeQueryPort.findById(EmployeeRoleMother.EMPLEADO.id()))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLEADO));
            when(roleQueryPort.findById(EmployeeRoleMother.ROL_RECEPCION.id()))
                    .thenReturn(Optional.of(EmployeeRoleMother.ROL_RECEPCION));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            UpdateEmployeeRoleCommand command = EmployeeRoleMother.comandoActualizarMismoEmpleado();
            service.execute(command);

            verify(permissionCachePort).evictByEmployeeId(EmployeeRoleMother.EMPLEADO.id());
            verifyNoMoreInteractions(permissionCachePort);
        }
    }

    @Nested
    @DisplayName("validaciones — no debe escribir")
    class Validaciones {

        @Test
        @DisplayName("una asignacion inexistente no toca los puertos de resolucion ni la cache")
        void asignacion_inexistente_no_toca_nada() {
            when(repository.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeRoleMother.comandoActualizar()))
                    .isInstanceOf(EmployeeRoleNotFoundException.class)
                    .hasMessageContaining(String.valueOf(EmployeeRoleMother.EMPLOYEE_ROLE_ID));

            verifyNoInteractions(employeeQueryPort, roleQueryPort, permissionCachePort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un empleado nuevo inexistente no toca el rol, el repositorio ni la cache")
        void empleado_nuevo_inexistente_no_toca_nada() {
            when(repository.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.habilitado()));
            when(employeeQueryPort.findById(EmployeeRoleMother.OTRO_EMPLEADO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeRoleMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Employee not found: " + EmployeeRoleMother.OTRO_EMPLEADO.id());

            verifyNoInteractions(roleQueryPort, permissionCachePort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un rol nuevo inexistente no toca el repositorio ni la cache")
        void rol_nuevo_inexistente_no_toca_repositorio_ni_cache() {
            when(repository.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.habilitado()));
            when(employeeQueryPort.findById(EmployeeRoleMother.OTRO_EMPLEADO.id()))
                    .thenReturn(Optional.of(EmployeeRoleMother.OTRO_EMPLEADO));
            when(roleQueryPort.findById(EmployeeRoleMother.ROL_RECEPCION.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeRoleMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Role not found: " + EmployeeRoleMother.ROL_RECEPCION.id());

            verifyNoInteractions(permissionCachePort);
            verify(repository, never()).save(any());
        }
    }
}
