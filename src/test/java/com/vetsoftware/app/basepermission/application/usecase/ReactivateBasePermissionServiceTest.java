package com.vetsoftware.app.basepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateBasePermissionService")
class ReactivateBasePermissionServiceTest {

    @Mock
    private BasePermissionRepository repository;
    @InjectMocks
    private ReactivateBasePermissionService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el permiso releido")
        void reactiva_y_devuelve_el_permiso_releido() {
            when(repository.reactivate(BasePermissionMother.BASE_PERMISSION_ID)).thenReturn(1);
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.of(BasePermissionMother.crearFactura()));

            BasePermissionDto dto = service.execute(BasePermissionMother.BASE_PERMISSION_ID);

            assertThat(dto.id()).isEqualTo(BasePermissionMother.BASE_PERMISSION_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BasePermissionNotFoundException si no reactivo ninguna fila y no relee")
        void lanza_not_found_si_no_reactivo_ninguna_fila() {
            when(repository.reactivate(BasePermissionMother.BASE_PERMISSION_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(BasePermissionMother.BASE_PERMISSION_ID))
                    .isInstanceOf(BasePermissionNotFoundException.class)
                    .hasMessageContaining("BasePermission not found with id: "
                            + BasePermissionMother.BASE_PERMISSION_ID);

            verify(repository, never()).findById(BasePermissionMother.BASE_PERMISSION_ID);
        }

        @Test
        @DisplayName("lanza BasePermissionNotFoundException si reactivo la fila pero desaparecio antes de la relectura")
        void lanza_not_found_si_desaparece_entre_reactivar_y_releer() {
            when(repository.reactivate(BasePermissionMother.BASE_PERMISSION_ID)).thenReturn(1);
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BasePermissionMother.BASE_PERMISSION_ID))
                    .isInstanceOf(BasePermissionNotFoundException.class)
                    .hasMessageContaining("BasePermission not found with id: "
                            + BasePermissionMother.BASE_PERMISSION_ID);
        }
    }
}
