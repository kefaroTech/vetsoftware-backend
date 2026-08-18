package com.vetsoftware.app.systempermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.out.SystemPermissionRepository;
import com.vetsoftware.app.systempermission.testsupport.SystemPermissionMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSystemPermissionsService")
class ListSystemPermissionsServiceTest {

    @Mock
    private SystemPermissionRepository repository;

    private ListSystemPermissionsService service;

    @BeforeEach
    void crearServicio() {
        service = new ListSystemPermissionsService(repository);
    }

    @Test
    @DisplayName("lista todos los permisos mapeados a dto")
    void lista_todos_los_permisos() {
        when(repository.findAll()).thenReturn(List.of(SystemPermissionMother.permisoValido()));

        List<SystemPermissionDto> resultado = service.listAll();

        assertThat(resultado).extracting(SystemPermissionDto::id).containsExactly(1L);
    }

    @Test
    @DisplayName("sin permisos recibe una lista vacia")
    void sin_permisos_recibe_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
