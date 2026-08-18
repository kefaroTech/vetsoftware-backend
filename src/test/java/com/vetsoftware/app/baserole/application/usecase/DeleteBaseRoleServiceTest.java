package com.vetsoftware.app.baserole.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserole.application.port.out.BaseRolePermissionChildrenQueryPort;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRoleHasActiveChildrenException;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
import com.vetsoftware.app.baserole.testsupport.BaseRoleMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteBaseRoleService")
class DeleteBaseRoleServiceTest {

    @Mock
    private BaseRoleRepository repository;
    @Mock
    private BaseRolePermissionChildrenQueryPort childrenQueryPort;

    @InjectMocks
    private DeleteBaseRoleService service;

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("sin hijos activos borra el rol")
        void sin_hijos_activos_borra_el_rol() {
            when(repository.findById(BaseRoleMother.BASE_ROLE_ID))
                    .thenReturn(Optional.of(BaseRoleMother.veterinario()));
            when(childrenQueryPort.existsActiveByBaseRoleId(BaseRoleMother.BASE_ROLE_ID))
                    .thenReturn(false);

            service.execute(BaseRoleMother.BASE_ROLE_ID);

            verify(repository).delete(BaseRoleMother.BASE_ROLE_ID);
        }
    }

    @Nested
    @DisplayName("rol inexistente")
    class RolInexistente {

        @Test
        @DisplayName("no consulta hijos ni borra")
        void no_consulta_hijos_ni_borra() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(99L))
                    .isInstanceOf(BaseRoleNotFoundException.class).hasMessageContaining("99");

            verify(childrenQueryPort, never()).existsActiveByBaseRoleId(99L);
            verify(repository, never()).delete(99L);
        }
    }

    @Nested
    @DisplayName("bloqueo por hijos activos")
    class BloqueoPorHijosActivos {

        @Test
        @DisplayName("con permisos de rol activos no borra")
        void con_permisos_de_rol_activos_no_borra() {
            when(repository.findById(BaseRoleMother.BASE_ROLE_ID))
                    .thenReturn(Optional.of(BaseRoleMother.veterinario()));
            when(childrenQueryPort.existsActiveByBaseRoleId(BaseRoleMother.BASE_ROLE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(BaseRoleMother.BASE_ROLE_ID))
                    .isInstanceOf(BaseRoleHasActiveChildrenException.class)
                    .hasMessageContaining("baseRolePermission");

            verify(repository, never()).delete(BaseRoleMother.BASE_ROLE_ID);
        }
    }
}
