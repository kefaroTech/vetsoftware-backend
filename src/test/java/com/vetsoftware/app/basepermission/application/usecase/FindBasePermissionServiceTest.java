package com.vetsoftware.app.basepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import com.vetsoftware.app.basepermission.testsupport.BasePermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindBasePermissionService")
class FindBasePermissionServiceTest {

    @Mock
    private BasePermissionRepository repository;
    @InjectMocks
    private FindBasePermissionService service;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el dto del permiso base encontrado")
        void devuelve_el_dto_del_permiso_encontrado() {
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.of(BasePermissionMother.crearFactura()));

            BasePermissionDto dto = service.findById(BasePermissionMother.BASE_PERMISSION_ID);

            assertThat(dto.id()).isEqualTo(BasePermissionMother.BASE_PERMISSION_ID);
            assertThat(dto.name()).isEqualTo("Crear factura");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BasePermissionNotFoundException si no existe")
        void lanza_not_found_si_no_existe() {
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .isInstanceOf(BasePermissionNotFoundException.class)
                    .hasMessageContaining("BasePermission not found with id: "
                            + BasePermissionMother.BASE_PERMISSION_ID);
        }
    }
}
