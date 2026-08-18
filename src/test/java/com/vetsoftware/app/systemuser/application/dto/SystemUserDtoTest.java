package com.vetsoftware.app.systemuser.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.systemuser.testsupport.SystemUserMother;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SystemUserDto.from")
class SystemUserDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        SystemUserDto dto = SystemUserDto.from(SystemUserMother.activo());

        assertThat(dto.id()).isEqualTo(SystemUserMother.SYSTEM_USER_ID);
        assertThat(dto.code()).isEqualTo(SystemUserMother.CODE);
        assertThat(dto.createdDate()).isEqualTo(SystemUserMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("propaga el usuario deshabilitado")
    void propaga_el_usuario_deshabilitado() {
        SystemUserDto dto = SystemUserDto.from(SystemUserMother.deshabilitado());

        assertThat(dto.enabled()).isFalse();
    }

    @Test
    @DisplayName("no expone hashPassword ni authVersion — el DTO es la vista publica")
    void no_expone_hash_password_ni_auth_version() {
        // El hash de la contrasena y el contador de rotacion de sesion son datos
        // sensibles del agregado: si algun dia se agregan como componentes del
        // record, este test los destapa antes de que lleguen a SystemUserResponse.
        List<String> nombresDeComponentes = Arrays.stream(SystemUserDto.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        assertThat(nombresDeComponentes).containsExactly("id", "code", "createdDate", "enabled");
    }
}
