package com.vetsoftware.app.systemuserpermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
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
@DisplayName("FindSystemUserPermissionService")
class FindSystemUserPermissionServiceTest {

    @Mock
    private SystemUserPermissionRepository repository;

    @InjectMocks
    private FindSystemUserPermissionService service;

    @Test
    @DisplayName("devuelve el dto de la asignacion encontrada")
    void devuelve_el_dto_de_la_asignacion_encontrada() {
        when(repository.findById(SystemUserPermissionMother.ID))
                .thenReturn(Optional.of(SystemUserPermissionMother.asignacionActiva()));

        SystemUserPermissionDto dto = service.findById(SystemUserPermissionMother.ID);

        assertThat(dto.id()).isEqualTo(SystemUserPermissionMother.ID);
        assertThat(dto.systemUser().code()).isEqualTo(SystemUserPermissionMother.USUARIO.code());
    }

    @Test
    @DisplayName("id inexistente lanza SystemUserPermissionNotFoundException")
    void id_inexistente_lanza_excepcion() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(SystemUserPermissionNotFoundException.class)
                .hasMessageContaining("999");
    }
}
