package com.vetsoftware.app.employeerole.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
@DisplayName("CreateEmployeeRoleService")
class CreateEmployeeRoleServiceTest {

    private static final Long EMPRESA = EmployeeRoleMother.EMPRESA;
    private static final Long EMPLEADO_ID = EmployeeRoleMother.EMPLEADO.id();
    private static final Long ROL_ID = EmployeeRoleMother.ROL_VETERINARIO.id();

    @Mock
    private EmployeeRoleRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private RoleQueryPort roleQueryPort;
    @Mock
    private PermissionCachePort permissionCachePort;

    @InjectMocks
    private CreateEmployeeRoleService service;

    @Captor
    private ArgumentCaptor<EmployeeRole> employeeRoleCaptor;

    @Nested
    @DisplayName("alta nueva — no habia asignacion previa")
    class Creacion {

        @Test
        @DisplayName("resuelve empleado y rol acotados por empresa, guarda y evita la cache")
        void guarda_la_asignacion_y_evita_la_cache() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO_ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLEADO));
            when(roleQueryPort.findByIdAndCompanyId(ROL_ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.ROL_VETERINARIO));
            when(repository.findDisabledIdByEmployeeAndRole(EMPLEADO_ID, ROL_ID))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(EmployeeRoleMother.habilitado());

            EmployeeRoleDto dto = service.execute(EmployeeRoleMother.comandoCrear());

            verify(repository).save(employeeRoleCaptor.capture());
            EmployeeRole guardado = employeeRoleCaptor.getValue();
            assertThat(guardado.getEmployee()).isEqualTo(EmployeeRoleMother.EMPLEADO);
            assertThat(guardado.getRole()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO);
            assertThat(guardado.isEnabled()).isTrue();
            assertThat(dto.id()).isEqualTo(EmployeeRoleMother.EMPLOYEE_ROLE_ID);
            verify(permissionCachePort).evictByEmployeeId(EMPLEADO_ID);
            verify(repository, never()).reactivate(any());
            verify(employeeQueryPort, never()).findById(any());
            verify(roleQueryPort, never()).findById(any());
        }

        @Test
        @DisplayName("sin empresa en el contexto (SYSTEM) resuelve sin acotar")
        void sin_empresa_resuelve_sin_acotar() {
            when(employeeQueryPort.findById(EMPLEADO_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLEADO));
            when(roleQueryPort.findById(ROL_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.ROL_VETERINARIO));
            when(repository.findDisabledIdByEmployeeAndRole(EMPLEADO_ID, ROL_ID))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(EmployeeRoleMother.habilitado());

            service.execute(EmployeeRoleMother.comandoCrearSinEmpresa());

            verify(repository).save(any());
            verify(employeeQueryPort, never()).findByIdAndCompanyId(any(), any());
            verify(roleQueryPort, never()).findByIdAndCompanyId(any(), any());
        }
    }

    @Nested
    @DisplayName("reactivacion — ya existia una asignacion deshabilitada")
    class Reactivacion {

        @Test
        @DisplayName("reactiva la fila desactivada de su empresa en lugar de insertar una nueva")
        void reactiva_en_lugar_de_insertar() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO_ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLEADO));
            when(roleQueryPort.findByIdAndCompanyId(ROL_ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.ROL_VETERINARIO));
            when(repository.findDisabledIdByEmployeeAndRole(EMPLEADO_ID, ROL_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLOYEE_ROLE_ID));
            when(repository.reactivate(EmployeeRoleMother.EMPLOYEE_ROLE_ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(EmployeeRoleMother.EMPLOYEE_ROLE_ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.habilitado()));

            EmployeeRoleDto dto = service.execute(EmployeeRoleMother.comandoCrear());

            verify(repository).reactivate(EmployeeRoleMother.EMPLOYEE_ROLE_ID, EMPRESA);
            verify(repository, never()).reactivate(anyLong());
            verify(repository, never()).save(any());
            verify(permissionCachePort).evictByEmployeeId(EMPLEADO_ID);
            assertThat(dto.id()).isEqualTo(EmployeeRoleMother.EMPLOYEE_ROLE_ID);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("por el camino SYSTEM reactiva con la consulta ancha")
        void sin_empresa_reactiva_sin_acotar() {
            when(employeeQueryPort.findById(EMPLEADO_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLEADO));
            when(roleQueryPort.findById(ROL_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.ROL_VETERINARIO));
            when(repository.findDisabledIdByEmployeeAndRole(EMPLEADO_ID, ROL_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLOYEE_ROLE_ID));
            when(repository.reactivate(EmployeeRoleMother.EMPLOYEE_ROLE_ID)).thenReturn(1);
            when(repository.findById(EmployeeRoleMother.EMPLOYEE_ROLE_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.habilitado()));

            service.execute(EmployeeRoleMother.comandoCrearSinEmpresa());

            verify(repository).reactivate(EmployeeRoleMother.EMPLOYEE_ROLE_ID);
            verify(repository, never()).reactivate(anyLong(), anyLong());
        }

        @Test
        @DisplayName("si tras reactivar no encuentra la fila, propaga el not found")
        void propaga_not_found_si_no_encuentra_tras_reactivar() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO_ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLEADO));
            when(roleQueryPort.findByIdAndCompanyId(ROL_ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.ROL_VETERINARIO));
            when(repository.findDisabledIdByEmployeeAndRole(EMPLEADO_ID, ROL_ID))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLOYEE_ROLE_ID));
            when(repository.reactivate(EmployeeRoleMother.EMPLOYEE_ROLE_ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(EmployeeRoleMother.EMPLOYEE_ROLE_ID, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeRoleMother.comandoCrear()))
                    .isInstanceOf(EmployeeRoleNotFoundException.class)
                    .hasMessageContaining(String.valueOf(EmployeeRoleMother.EMPLOYEE_ROLE_ID));
            verifyNoInteractions(permissionCachePort);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * El defecto que cierra este test (W6). El gate del puerto solo llevaba
         * {@code hasAuthority('employee.create')}: cualquier administrador con permiso
         * de alta podia asignarle un rol a un empleado de otra empresa adivinando su
         * id, y el {@code evictByEmployeeId} se lo hacia efectivo en el acto. No era la
         * carga de la fila propia —en un alta no hay— sino la resolucion de la
         * referencia entrante.
         */
        @Test
        @DisplayName("un empleado de otra empresa se rechaza y no escribe ni el rol ni la cache")
        void no_se_puede_asignar_un_rol_a_un_empleado_de_otra_empresa() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO_ID, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeRoleMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + EMPLEADO_ID);

            verify(employeeQueryPort, never()).findById(any());
            verifyNoInteractions(roleQueryPort, repository, permissionCachePort);
        }

        /**
         * Acotar solo el empleado no basta: los roles tienen {@code company_id}, asi
         * que colgar el rol de B de un empleado propio le entrega los permisos que la
         * membresia de A no autoriza.
         */
        @Test
        @DisplayName("un rol de otra empresa se rechaza y no escribe nada")
        void no_se_puede_asignar_un_rol_de_otra_empresa() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO_ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLEADO));
            when(roleQueryPort.findByIdAndCompanyId(ROL_ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeRoleMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found: " + ROL_ID);

            verify(roleQueryPort, never()).findById(any());
            verifyNoInteractions(repository, permissionCachePort);
        }
    }

    @Nested
    @DisplayName("validaciones — no debe escribir")
    class Validaciones {

        @Test
        @DisplayName("un empleado inexistente no toca el rol, el repositorio ni la cache")
        void empleado_inexistente_no_toca_nada() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO_ID, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeRoleMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + EMPLEADO_ID);

            verifyNoInteractions(roleQueryPort, repository, permissionCachePort);
        }

        @Test
        @DisplayName("un rol inexistente no toca el repositorio ni la cache")
        void rol_inexistente_no_toca_repositorio_ni_cache() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO_ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.EMPLEADO));
            when(roleQueryPort.findByIdAndCompanyId(ROL_ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(EmployeeRoleMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found: " + ROL_ID);

            verifyNoInteractions(repository, permissionCachePort);
        }
    }
}
