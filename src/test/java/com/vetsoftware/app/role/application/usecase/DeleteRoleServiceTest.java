package com.vetsoftware.app.role.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.application.port.out.EmployeeRoleChildrenQueryPort;
import com.vetsoftware.app.role.application.port.out.RolePermissionChildrenCascadePort;
import com.vetsoftware.app.role.application.port.out.RoleRepository;
import com.vetsoftware.app.role.domain.RoleHasActiveChildrenException;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
import com.vetsoftware.app.role.testsupport.RoleMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteRoleService")
class DeleteRoleServiceTest {

    private static final Long ROLE_ID = RoleMother.ROLE_ID;
    private static final Long COMPANY_ID = RoleMother.COMPANY_ID;

    @Mock
    private RoleRepository repository;
    @Mock
    private EmployeeRoleChildrenQueryPort employeeRoleChildrenQueryPort;
    @Mock
    private RolePermissionChildrenCascadePort rolePermissionChildrenCascadePort;

    private DeleteRoleService service;

    @BeforeEach
    void crearServicio() {
        service = new DeleteRoleService(repository, employeeRoleChildrenQueryPort,
                rolePermissionChildrenCascadePort);
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("sin empleados asignados, desactiva los permisos y borra el rol")
        void sin_empleados_asignados_desactiva_permisos_y_borra() {
            when(repository.findByIdAndCompanyId(ROLE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(RoleMother.veterinario()));
            when(employeeRoleChildrenQueryPort.existsActiveByRoleId(ROLE_ID)).thenReturn(false);

            service.execute(ROLE_ID, COMPANY_ID);

            verify(rolePermissionChildrenCascadePort).deactivateAllByRoleId(ROLE_ID, COMPANY_ID);
            verify(repository).delete(ROLE_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("con empleados con ese rol activos, rechaza el borrado sin tocar el repositorio")
        void con_empleados_activos_rechaza_el_borrado() {
            when(repository.findByIdAndCompanyId(ROLE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(RoleMother.veterinario()));
            when(employeeRoleChildrenQueryPort.existsActiveByRoleId(ROLE_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(ROLE_ID, COMPANY_ID))
                    .isInstanceOf(RoleHasActiveChildrenException.class)
                    .hasMessageContaining("employeeRole");

            verifyNoInteractions(rolePermissionChildrenCascadePort);
            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("tenancy")
    class Tenancy {

        @Test
        @DisplayName("un rol de otra empresa no se encuentra: no cae a una busqueda global")
        void un_rol_de_otra_empresa_no_se_encuentra() {
            Long otraCompanyId = 77L;
            when(repository.findByIdAndCompanyId(ROLE_ID, otraCompanyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ROLE_ID, otraCompanyId))
                    .isInstanceOf(RoleNotFoundException.class);

            verify(repository).findByIdAndCompanyId(ROLE_ID, otraCompanyId);
            verifyNoMoreInteractions(repository, employeeRoleChildrenQueryPort,
                    rolePermissionChildrenCascadePort);
        }
    }
}
