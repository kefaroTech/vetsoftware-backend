package com.vetsoftware.app.systemuser.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import com.vetsoftware.app.systemuser.application.command.CreateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUser;
import com.vetsoftware.app.systemuser.testsupport.SystemUserMother;
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
@DisplayName("CreateSystemUserService")
class CreateSystemUserServiceTest {

    @Mock
    private SystemUserRepository repository;
    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private CreateSystemUserService service;

    @Captor
    private ArgumentCaptor<SystemUser> systemUserCaptor;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el usuario con el hash devuelto por el puerto, nunca la contrasena en claro")
        void persiste_el_usuario_con_el_hash_devuelto_por_el_puerto() {
            when(passwordHasher.hash("unaContrasenaSegura1"))
                    .thenReturn("hash-devuelto-por-el-puerto");
            when(repository.save(any())).thenReturn(SystemUserMother.activo());

            service.execute(SystemUserMother.comandoCrear());

            verify(repository).save(systemUserCaptor.capture());
            SystemUser guardado = systemUserCaptor.getValue();
            assertThat(guardado.getHashPassword()).isEqualTo("hash-devuelto-por-el-puerto");
            assertThat(guardado.getCode()).isEqualTo(SystemUserMother.CODE);
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("devuelve el DTO del usuario ya persistido, con su id")
        void devuelve_el_dto_del_usuario_ya_persistido() {
            when(passwordHasher.hash(any())).thenReturn("hash-devuelto-por-el-puerto");
            when(repository.save(any())).thenReturn(SystemUserMother.activo());

            SystemUserDto dto = service.execute(SystemUserMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(SystemUserMother.SYSTEM_USER_ID);
            assertThat(dto.code()).isEqualTo(SystemUserMother.CODE);
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("un code vacio no llega a persistirse")
        void un_code_vacio_no_llega_a_persistirse() {
            // El hash se pide ANTES de construir el agregado, asi que el puerto se
            // invoca aunque el comando termine siendo invalido.
            when(passwordHasher.hash(any())).thenReturn("hash-devuelto-por-el-puerto");
            CreateSystemUserCommand comando = new CreateSystemUserCommand("   ",
                    "unaContrasenaSegura1");

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");

            verify(repository, never()).save(any());
        }
    }
}
