package com.vetsoftware.app.permission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import com.vetsoftware.app.permission.testsupport.PermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindPermissionService")
class FindPermissionServiceTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private FindPermissionService service;

    @Test
    @DisplayName("devuelve el permiso existente mapeado a DTO")
    void devuelve_el_permiso_existente() {
        when(repository.findById(PermissionMother.PERMISSION_ID))
                .thenReturn(Optional.of(PermissionMother.permisoValido()));

        PermissionDto dto = service.findById(PermissionMother.PERMISSION_ID);

        assertThat(dto.id()).isEqualTo(PermissionMother.PERMISSION_ID);
        assertThat(dto.name()).isEqualTo("Crear factura");
    }

    @Test
    @DisplayName("permiso inexistente lanza PermissionNotFoundException")
    void permiso_inexistente_lanza_excepcion() {
        when(repository.findById(PermissionMother.PERMISSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(PermissionMother.PERMISSION_ID))
                .isInstanceOf(PermissionNotFoundException.class)
                .hasMessageContaining(String.valueOf(PermissionMother.PERMISSION_ID));
    }
}
