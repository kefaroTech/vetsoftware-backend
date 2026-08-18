package com.vetsoftware.app.basepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.testsupport.BasePermissionMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListBasePermissionsService")
class ListBasePermissionsServiceTest {

    @Mock
    private BasePermissionRepository repository;
    @InjectMocks
    private ListBasePermissionsService service;

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada permiso base del repositorio a su dto")
        void mapea_cada_permiso_a_su_dto() {
            when(repository.findAll()).thenReturn(List.of(BasePermissionMother.crearFactura()));

            List<BasePermissionDto> dtos = service.listAll();

            assertThat(dtos).extracting(BasePermissionDto::name).containsExactly("Crear factura");
        }

        @Test
        @DisplayName("una lista vacia en el repositorio devuelve una lista vacia")
        void lista_vacia_devuelve_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.listAll()).isEmpty();
        }
    }
}
