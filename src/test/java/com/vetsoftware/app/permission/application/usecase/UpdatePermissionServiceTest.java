package com.vetsoftware.app.permission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.application.command.UpdatePermissionCommand;
import com.vetsoftware.app.permission.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
import com.vetsoftware.app.permission.testsupport.PermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePermissionService")
class UpdatePermissionServiceTest {

    @Mock
    private PermissionRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private SubModuleQueryPort subModuleQueryPort;

    @InjectMocks
    private UpdatePermissionService service;

    @Captor
    private ArgumentCaptor<Permission> permissionCaptor;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el permiso existente con las referencias resueltas")
        void actualiza_el_permiso_existente() {
            Permission existente = PermissionMother.permisoValido();
            when(repository.findById(PermissionMother.PERMISSION_ID))
                    .thenReturn(Optional.of(existente));
            when(companyQueryPort.findById(PermissionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PermissionMother.OTRA_CLINICA));
            when(subModuleQueryPort.findById(PermissionMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(PermissionMother.FACTURACION));
            when(repository.save(any())).thenReturn(existente);

            service.execute(PermissionMother.comandoActualizar());

            verify(repository).save(permissionCaptor.capture());
            Permission guardado = permissionCaptor.getValue();
            assertThat(guardado.getName()).isEqualTo("Editar factura");
            assertThat(guardado.getCode()).isEqualTo("billing.update");
            assertThat(guardado.getCompany()).isEqualTo(PermissionMother.OTRA_CLINICA);
            assertThat(guardado.getSubModule()).isEqualTo(PermissionMother.FACTURACION);
            // La misma instancia leida por findById es la que se muta y se guarda.
            assertThat(guardado).isSameAs(existente);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("permiso inexistente: no consulta los demas puertos")
        void permiso_inexistente() {
            when(repository.findById(PermissionMother.PERMISSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PermissionMother.comandoActualizar()))
                    .isInstanceOf(PermissionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PermissionMother.PERMISSION_ID));

            verifyNoInteractions(companyQueryPort, subModuleQueryPort);
        }

        @Test
        @DisplayName("empresa inexistente: no guarda")
        void empresa_inexistente() {
            when(repository.findById(PermissionMother.PERMISSION_ID))
                    .thenReturn(Optional.of(PermissionMother.permisoValido()));
            when(companyQueryPort.findById(PermissionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PermissionMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + PermissionMother.COMPANY_ID);

            verifyNoInteractions(subModuleQueryPort);
        }

        @Test
        @DisplayName("submodulo inexistente: no guarda")
        void submodulo_inexistente() {
            when(repository.findById(PermissionMother.PERMISSION_ID))
                    .thenReturn(Optional.of(PermissionMother.permisoValido()));
            when(companyQueryPort.findById(PermissionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PermissionMother.CLINICA));
            when(subModuleQueryPort.findById(PermissionMother.SUB_MODULE_ID))
                    .thenReturn(Optional.empty());

            UpdatePermissionCommand command = PermissionMother.comandoActualizar();

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SubModule not found: " + PermissionMother.SUB_MODULE_ID);
        }
    }
}
