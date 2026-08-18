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
@DisplayName("FindBaseRoleService")
class FindBaseRoleServiceTest {

    @Mock
    private BaseRoleRepository repository;

    @InjectMocks
    private FindBaseRoleService service;

    @Nested
    @DisplayName("rol existente")
    class RolExistente {

        @Test
        @DisplayName("devuelve el dto del rol encontrado")
        void devuelve_el_dto_del_rol_encontrado() {
            when(repository.findById(BaseRoleMother.BASE_ROLE_ID))
                    .thenReturn(Optional.of(BaseRoleMother.veterinario()));

            BaseRoleDto result = service.findById(BaseRoleMother.BASE_ROLE_ID);

            assertThat(result.id()).isEqualTo(BaseRoleMother.BASE_ROLE_ID);
            assertThat(result.name()).isEqualTo("Veterinario");
        }
    }

    @Nested
    @DisplayName("rol inexistente")
    class RolInexistente {

        @Test
        @DisplayName("lanza not found")
        void lanza_not_found() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(BaseRoleNotFoundException.class).hasMessageContaining("99");
        }
    }
}
