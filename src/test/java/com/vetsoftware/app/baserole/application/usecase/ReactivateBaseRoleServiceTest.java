package com.vetsoftware.app.baserole.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
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
@DisplayName("ReactivateBaseRoleService")
class ReactivateBaseRoleServiceTest {

    @Mock
    private BaseRoleRepository repository;

    @InjectMocks
    private ReactivateBaseRoleService service;

    @Nested
    @DisplayName("reactivacion permitida")
    class ReactivacionPermitida {

        @Test
        @DisplayName("reactiva y devuelve el rol actualizado")
        void reactiva_y_devuelve_el_rol_actualizado() {
            when(repository.reactivate(BaseRoleMother.BASE_ROLE_ID)).thenReturn(1);
            when(repository.findById(BaseRoleMother.BASE_ROLE_ID))
                    .thenReturn(Optional.of(BaseRoleMother.veterinario()));

            BaseRoleDto result = service.execute(BaseRoleMother.BASE_ROLE_ID);

            assertThat(result.id()).isEqualTo(BaseRoleMother.BASE_ROLE_ID);
        }
    }

    @Nested
    @DisplayName("rol inexistente")
    class RolInexistente {

        @Test
        @DisplayName("cero filas afectadas lanza not found")
        void cero_filas_afectadas_lanza_not_found() {
            when(repository.reactivate(99L)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(99L))
                    .isInstanceOf(BaseRoleNotFoundException.class).hasMessageContaining("99");
        }
    }
}
