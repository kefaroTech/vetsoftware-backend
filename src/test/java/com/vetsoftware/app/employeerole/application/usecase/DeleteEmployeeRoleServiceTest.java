package com.vetsoftware.app.employeerole.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.application.port.out.PermissionCachePort;
import com.vetsoftware.app.employeerole.domain.EmployeeRoleNotFoundException;
import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteEmployeeRoleService")
class DeleteEmployeeRoleServiceTest {

    private static final Long EMPRESA = 9L;
    private static final Long ID = EmployeeRoleMother.EMPLOYEE_ROLE_ID;

    @Mock
    private EmployeeRoleRepository repository;
    @Mock
    private PermissionCachePort permissionCachePort;

    @InjectMocks
    private DeleteEmployeeRoleService service;

    @Nested
    @DisplayName("baja valida")
    class Baja {

        @Test
        @DisplayName("elimina la asignacion de su empresa y evita la cache del empleado titular")
        void elimina_y_evita_la_cache() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.habilitado()));

            service.execute(ID, EMPRESA);

            verify(repository).delete(ID);
            verify(permissionCachePort).evictByEmployeeId(EmployeeRoleMother.EMPLEADO.id());
        }

        @Test
        @DisplayName("sin empresa en el contexto (SYSTEM) carga sin acotar")
        void sin_empresa_carga_sin_acotar() {
            when(repository.findById(ID)).thenReturn(Optional.of(EmployeeRoleMother.habilitado()));

            service.execute(ID, null);

            verify(repository).delete(ID);
            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * El defecto que cierra este test: cargando por id a secas, un empleado podia
         * revocarle el rol al administrador de otra empresa.
         */
        @Test
        @DisplayName("una asignacion de otra empresa no se borra ni toca la cache")
        void asignacion_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(EmployeeRoleNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).delete(any());
            verify(repository, never()).findById(any());
            verifyNoInteractions(permissionCachePort);
        }
    }

    @Nested
    @DisplayName("validaciones — no debe escribir")
    class Validaciones {

        @Test
        @DisplayName("una asignacion inexistente no borra nada ni toca la cache")
        void asignacion_inexistente_no_borra_nada() {
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(EmployeeRoleNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).delete(any());
            verifyNoInteractions(permissionCachePort);
        }
    }
}
