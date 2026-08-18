package com.vetsoftware.app.systemuser.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuser.application.command.UpdateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUser;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import com.vetsoftware.app.systemuser.testsupport.SystemUserMother;
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
@DisplayName("UpdateSystemUserService")
class UpdateSystemUserServiceTest {

    @Mock
    private SystemUserRepository repository;

    @InjectMocks
    private UpdateSystemUserService service;

    @Captor
    private ArgumentCaptor<SystemUser> systemUserCaptor;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("aplica el nuevo code al agregado encontrado y lo persiste")
        void aplica_el_nuevo_code_y_lo_persiste() {
            SystemUser existente = SystemUserMother.activo();
            when(repository.findById(SystemUserMother.SYSTEM_USER_ID))
                    .thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SystemUserDto dto = service.execute(SystemUserMother.comandoActualizar());

            verify(repository).save(systemUserCaptor.capture());
            assertThat(systemUserCaptor.getValue().getCode()).isEqualTo("svc-actualizado");
            assertThat(dto.code()).isEqualTo("svc-actualizado");
        }
    }

    @Nested
    @DisplayName("usuario inexistente")
    class UsuarioInexistente {

        @Test
        @DisplayName("no encontrado no persiste nada")
        void no_encontrado_no_persiste_nada() {
            when(repository.findById(SystemUserMother.SYSTEM_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SystemUserMother.comandoActualizar()))
                    .isInstanceOf(SystemUserNotFoundException.class).hasMessageContaining(
                            "SystemUser not found: " + SystemUserMother.SYSTEM_USER_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un code invalido aborta la actualizacion sin persistir")
        void un_code_invalido_aborta_la_actualizacion() {
            when(repository.findById(SystemUserMother.SYSTEM_USER_ID))
                    .thenReturn(Optional.of(SystemUserMother.activo()));

            assertThatThrownBy(() -> service
                    .execute(new UpdateSystemUserCommand(SystemUserMother.SYSTEM_USER_ID, "   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");

            verify(repository, never()).save(any());
        }
    }
}
