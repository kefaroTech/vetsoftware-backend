package com.vetsoftware.app.baserolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.out.BaseRolePermissionRepository;
import com.vetsoftware.app.baserolepermission.testsupport.BaseRolePermissionMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListBaseRolePermissionsService")
class ListBaseRolePermissionsServiceTest {

    @Mock
    private BaseRolePermissionRepository repository;
    @InjectMocks
    private ListBaseRolePermissionsService service;

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada vinculo del repositorio a su dto")
        void mapea_cada_vinculo_a_su_dto() {
            when(repository.findAll()).thenReturn(List.of(BaseRolePermissionMother.vinculo()));

            List<BaseRolePermissionDto> dtos = service.listAll();

            assertThat(dtos).extracting(dto -> dto.baseRole().code()).containsExactly("VET");
        }

        @Test
        @DisplayName("una lista vacia en el repositorio devuelve una lista vacia")
        void lista_vacia_devuelve_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.listAll()).isEmpty();
        }
    }
}
