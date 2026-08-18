package com.vetsoftware.app.systemuser.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateSystemUserService")
class ReactivateSystemUserServiceTest {

    @Mock
    private SystemUserRepository repository;

    @InjectMocks
    private ReactivateSystemUserService service;

    @Test
    @DisplayName("reactiva y devuelve el usuario ya habilitado")
    void reactiva_y_devuelve_el_usuario_ya_habilitado() {
        when(repository.reactivate(SystemUserMother.SYSTEM_USER_ID)).thenReturn(1);
        when(repository.findById(SystemUserMother.SYSTEM_USER_ID))
                .thenReturn(Optional.of(SystemUserMother.activo()));

        SystemUserDto dto = service.execute(SystemUserMother.SYSTEM_USER_ID);

        assertThat(dto.id()).isEqualTo(SystemUserMother.SYSTEM_USER_ID);
        assertThat(dto.enabled()).isTrue();
        verify(repository).reactivate(SystemUserMother.SYSTEM_USER_ID);
    }

    @Test
    @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
    void cero_filas_afectadas_es_no_encontrado() {
        // El UPDATE ya es la unica fuente de verdad de "existe": 0 filas cubre a la
        // vez "no existe" y cualquier otra razon por la que no se pudo actualizar. No
        // hace falta un SELECT previo.
        when(repository.reactivate(SystemUserMother.SYSTEM_USER_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(SystemUserMother.SYSTEM_USER_ID))
                .isInstanceOf(SystemUserNotFoundException.class)
                .hasMessageContaining("SystemUser not found: " + SystemUserMother.SYSTEM_USER_ID);

        verify(repository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("si el usuario desaparece entre el UPDATE y el SELECT, falla como no-encontrado")
    void si_el_usuario_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(SystemUserMother.SYSTEM_USER_ID)).thenReturn(1);
        when(repository.findById(SystemUserMother.SYSTEM_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SystemUserMother.SYSTEM_USER_ID))
                .isInstanceOf(SystemUserNotFoundException.class);
    }
}
