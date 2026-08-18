package com.vetsoftware.app.basepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
import com.vetsoftware.app.basepermission.testsupport.BasePermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateBasePermissionService")
class UpdateBasePermissionServiceTest {

    @Mock
    private BasePermissionRepository repository;
    @Mock
    private SubModuleQueryPort subModuleQueryPort;
    @InjectMocks
    private UpdateBasePermissionService service;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza nombre, codigo y submodulo y persiste el agregado")
        void actualiza_nombre_codigo_y_submodulo_y_persiste_el_agregado() {
            BasePermission existente = BasePermissionMother.crearFactura();
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.of(existente));
            when(subModuleQueryPort.findById(BasePermissionMother.INVENTARIO.id()))
                    .thenReturn(Optional.of(BasePermissionMother.INVENTARIO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BasePermissionDto dto = service.execute(BasePermissionMother.comandoActualizar());

            ArgumentCaptor<BasePermission> captor = ArgumentCaptor.forClass(BasePermission.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Editar factura");
            assertThat(captor.getValue().getCode()).isEqualTo("INVOICE_UPDATE");
            assertThat(captor.getValue().getSubModule()).isEqualTo(BasePermissionMother.INVENTARIO);
            assertThat(dto.name()).isEqualTo("Editar factura");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BasePermissionNotFoundException si el permiso no existe y no consulta el submodulo")
        void lanza_not_found_si_el_permiso_no_existe() {
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BasePermissionMother.comandoActualizar()))
                    .isInstanceOf(BasePermissionNotFoundException.class)
                    .hasMessageContaining("BasePermission not found with id: "
                            + BasePermissionMother.BASE_PERMISSION_ID);

            verifyNoInteractions(subModuleQueryPort);
        }

        @Test
        @DisplayName("no guarda si el submodulo destino no existe")
        void no_guarda_si_el_submodulo_destino_no_existe() {
            when(repository.findById(BasePermissionMother.BASE_PERMISSION_ID))
                    .thenReturn(Optional.of(BasePermissionMother.crearFactura()));
            when(subModuleQueryPort.findById(BasePermissionMother.INVENTARIO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BasePermissionMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "SubModule not found: " + BasePermissionMother.INVENTARIO.id());

            verify(repository, never()).save(any());
        }
    }
}
