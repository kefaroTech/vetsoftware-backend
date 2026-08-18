package com.vetsoftware.app.baserole.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserole.application.command.UpdateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRole;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
import com.vetsoftware.app.baserole.testsupport.BaseRoleMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateBaseRoleService")
class UpdateBaseRoleServiceTest {

    @Mock
    private BaseRoleRepository repository;

    @InjectMocks
    private UpdateBaseRoleService service;

    @Nested
    @DisplayName("actualizacion permitida")
    class ActualizacionPermitida {

        @Test
        @DisplayName("actualiza los campos y persiste el agregado")
        void actualiza_los_campos_y_persiste_el_agregado() {
            BaseRole existente = BaseRoleMother.veterinario();
            when(repository.findById(BaseRoleMother.BASE_ROLE_ID))
                    .thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BaseRoleDto result = service.execute(new UpdateBaseRoleCommand(
                    BaseRoleMother.BASE_ROLE_ID, "Administrador", "ADMIN", true));

            assertThat(result.name()).isEqualTo("Administrador");
            assertThat(result.code()).isEqualTo("ADMIN");
            assertThat(result.mandatory()).isTrue();

            ArgumentCaptor<BaseRole> captor = ArgumentCaptor.forClass(BaseRole.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(BaseRoleMother.BASE_ROLE_ID);
        }
    }

    @Nested
    @DisplayName("rol inexistente")
    class RolInexistente {

        @Test
        @DisplayName("no encontrado no toca el repositorio para guardar")
        void no_encontrado_no_toca_el_repositorio_para_guardar() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new UpdateBaseRoleCommand(99L, "Administrador", "ADMIN", true)))
                    .isInstanceOf(BaseRoleNotFoundException.class).hasMessageContaining("99");
        }
    }
}
