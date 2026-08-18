package com.vetsoftware.app.permission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.testsupport.PermissionMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListPermissionsService")
class ListPermissionsServiceTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private ListPermissionsService service;

    @Test
    @DisplayName("lista todos los permisos mapeados a DTO")
    void lista_todos_los_permisos() {
        when(repository.findAll()).thenReturn(List.of(PermissionMother.permisoValido()));

        List<PermissionDto> dtos = service.listAll();

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).id()).isEqualTo(PermissionMother.PERMISSION_ID);
    }

    @Test
    @DisplayName("sin permisos devuelve lista vacia")
    void sin_permisos_devuelve_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
