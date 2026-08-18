package com.vetsoftware.app.systemuserpermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import com.vetsoftware.app.systemuserpermission.testsupport.SystemUserPermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSystemUserPermissionService")
class DeleteSystemUserPermissionServiceTest {

    @Mock
    private SystemUserPermissionRepository repository;

    @InjectMocks
    private DeleteSystemUserPermissionService service;

    @Test
    @DisplayName("borra la asignacion existente")
    void borra_la_asignacion_existente() {
        when(repository.findById(SystemUserPermissionMother.ID))
                .thenReturn(Optional.of(SystemUserPermissionMother.asignacionActiva()));

        service.execute(SystemUserPermissionMother.ID);

        verify(repository).delete(SystemUserPermissionMother.ID);
    }

    @Test
    @DisplayName("asignacion inexistente: no llega a borrar")
    void asignacion_inexistente_no_llega_a_borrar() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(999L))
                .isInstanceOf(SystemUserPermissionNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).delete(anyLong());
    }
}
