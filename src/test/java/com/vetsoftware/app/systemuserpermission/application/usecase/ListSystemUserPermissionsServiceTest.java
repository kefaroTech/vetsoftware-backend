package com.vetsoftware.app.systemuserpermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.testsupport.SystemUserPermissionMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSystemUserPermissionsService")
class ListSystemUserPermissionsServiceTest {

    @Mock
    private SystemUserPermissionRepository repository;

    @InjectMocks
    private ListSystemUserPermissionsService service;

    @Test
    @DisplayName("mapea cada asignacion a su dto conservando el orden del repositorio")
    void mapea_cada_asignacion_conservando_el_orden() {
        SystemUserPermission primera = SystemUserPermissionMother.asignacionActiva(1L);
        SystemUserPermission segunda = SystemUserPermissionMother.asignacionActiva(2L);
        when(repository.findAll()).thenReturn(List.of(primera, segunda));

        List<SystemUserPermissionDto> dtos = service.listAll();

        assertThat(dtos).extracting(SystemUserPermissionDto::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("una lista vacia no es un error")
    void una_lista_vacia_no_es_un_error() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
