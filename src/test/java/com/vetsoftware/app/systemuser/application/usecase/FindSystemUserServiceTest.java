package com.vetsoftware.app.systemuser.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.out.SystemUserRepository;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import com.vetsoftware.app.systemuser.testsupport.SystemUserMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindSystemUserService")
class FindSystemUserServiceTest {

    @Mock
    private SystemUserRepository repository;

    @InjectMocks
    private FindSystemUserService service;

    @Test
    @DisplayName("devuelve el DTO del usuario encontrado")
    void devuelve_el_dto_del_usuario_encontrado() {
        when(repository.findById(SystemUserMother.SYSTEM_USER_ID))
                .thenReturn(Optional.of(SystemUserMother.activo()));

        SystemUserDto dto = service.findById(SystemUserMother.SYSTEM_USER_ID);

        assertThat(dto.id()).isEqualTo(SystemUserMother.SYSTEM_USER_ID);
        assertThat(dto.code()).isEqualTo(SystemUserMother.CODE);
    }

    @Test
    @DisplayName("un usuario inexistente responde con SystemUserNotFoundException")
    void un_usuario_inexistente_responde_con_not_found() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(SystemUserNotFoundException.class)
                .hasMessageContaining("SystemUser not found: 999");
    }
}
