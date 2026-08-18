package com.vetsoftware.app.permission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.permission.application.port.out.PermissionRepository;
import com.vetsoftware.app.permission.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.permission.domain.Permission;
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
@DisplayName("CreatePermissionService")
class CreatePermissionServiceTest {

    @Mock
    private PermissionRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private SubModuleQueryPort subModuleQueryPort;

    @InjectMocks
    private CreatePermissionService service;

    @Captor
    private ArgumentCaptor<Permission> permissionCaptor;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el permiso con las referencias resueltas por los puertos")
        void persiste_el_permiso_con_las_referencias_resueltas() {
            when(companyQueryPort.findById(PermissionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PermissionMother.CLINICA));
            when(subModuleQueryPort.findById(PermissionMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(PermissionMother.INVENTARIO));
            when(repository.save(any())).thenReturn(PermissionMother.permisoValido());

            PermissionDto dto = service.execute(PermissionMother.comandoCrear());

            verify(repository).save(permissionCaptor.capture());
            Permission guardado = permissionCaptor.getValue();
            assertThat(guardado.getName()).isEqualTo("Crear factura");
            assertThat(guardado.getCode()).isEqualTo("billing.create");
            assertThat(guardado.getCompany()).isEqualTo(PermissionMother.CLINICA);
            assertThat(guardado.getSubModule()).isEqualTo(PermissionMother.INVENTARIO);
            assertThat(guardado.getId()).isNull();
            assertThat(dto.id()).isEqualTo(PermissionMother.PERMISSION_ID);
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("empresa inexistente: no consulta el submodulo ni persiste")
        void empresa_inexistente() {
            when(companyQueryPort.findById(PermissionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PermissionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + PermissionMother.COMPANY_ID);

            verifyNoInteractions(subModuleQueryPort, repository);
        }

        @Test
        @DisplayName("submodulo inexistente: no persiste")
        void submodulo_inexistente() {
            when(companyQueryPort.findById(PermissionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PermissionMother.CLINICA));
            when(subModuleQueryPort.findById(PermissionMother.SUB_MODULE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PermissionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SubModule not found: " + PermissionMother.SUB_MODULE_ID);

            verifyNoInteractions(repository);
        }
    }
}
