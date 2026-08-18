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
@DisplayName("ReactivateEmployeeRoleService")
class ReactivateEmployeeRoleServiceTest {

    private static final Long EMPRESA = 9L;
    private static final Long ID = EmployeeRoleMother.EMPLOYEE_ROLE_ID;

    @Mock
    private EmployeeRoleRepository repository;
    @Mock
    private PermissionCachePort permissionCachePort;

    @InjectMocks
    private ReactivateEmployeeRoleService service;

    @Nested
    @DisplayName("reactivacion valida")
    class Reactivacion {

        @Test
        @DisplayName("reactiva la fila de su empresa y evita la cache del empleado titular")
        void reactiva_y_evita_la_cache() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ID, EMPRESA))
                    .thenReturn(Optional.of(EmployeeRoleMother.habilitado()));

            EmployeeRoleDto dto = service.execute(ID, EMPRESA);

            assertThat(dto.id()).isEqualTo(ID);
            assertThat(dto.enabled()).isTrue();
            verify(permissionCachePort).evictByEmployeeId(EmployeeRoleMother.EMPLEADO.id());
        }

        @Test
        @DisplayName("sin empresa en el contexto (SYSTEM) reactiva sin acotar")
        void sin_empresa_reactiva_sin_acotar() {
            when(repository.reactivate(ID)).thenReturn(1);
            when(repository.findById(ID)).thenReturn(Optional.of(EmployeeRoleMother.habilitado()));

            assertThat(service.execute(ID, null).id()).isEqualTo(ID);

            verify(repository, never()).reactivate(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * El defecto que cierra este test: sin el companyId en el UPDATE, el id de otra
         * empresa afectaba una fila y el empleado ajeno recuperaba un privilegio que su
         * propio administrador le habia revocado.
         */
        @Test
        @DisplayName("una asignacion de otra empresa no reactiva nada ni toca la cache")
        void asignacion_de_otra_empresa_no_reactiva_nada() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(EmployeeRoleNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).reactivate(anyLong());
            verify(repository, never()).findById(any());
            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verifyNoInteractions(permissionCachePort);
        }
    }

    @Nested
    @DisplayName("validaciones — no debe escribir")
    class Validaciones {

        @Test
        @DisplayName("si tras reactivar no encuentra la fila, propaga EmployeeRoleNotFoundException")
        void tras_reactivar_no_encuentra_la_fila() {
            when(repository.reactivate(ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(ID, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ID, EMPRESA))
                    .isInstanceOf(EmployeeRoleNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verifyNoInteractions(permissionCachePort);
        }
    }
}
