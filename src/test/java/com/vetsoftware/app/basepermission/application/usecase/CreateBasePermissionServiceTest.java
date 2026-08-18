package com.vetsoftware.app.basepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.out.AdminPermissionPublisher;
import com.vetsoftware.app.basepermission.application.port.out.BasePermissionRepository;
import com.vetsoftware.app.basepermission.application.port.out.MandatoryBaseRolePermissionInitializationPort;
import com.vetsoftware.app.basepermission.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.basepermission.domain.BasePermission;
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
@DisplayName("CreateBasePermissionService")
class CreateBasePermissionServiceTest {

    @Mock
    private BasePermissionRepository repository;
    @Mock
    private SubModuleQueryPort subModuleQueryPort;
    @Mock
    private MandatoryBaseRolePermissionInitializationPort mandatoryBaseRolePermissionInitializationPort;
    @Mock
    private AdminPermissionPublisher adminPermissionPublisher;
    @InjectMocks
    private CreateBasePermissionService service;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el permiso con el submodulo resuelto, inicializa los roles mandatorios y publica")
        void persiste_el_permiso_inicializa_roles_y_publica() {
            when(subModuleQueryPort.findById(BasePermissionMother.VENTAS.id()))
                    .thenReturn(Optional.of(BasePermissionMother.VENTAS));
            when(repository.save(any())).thenAnswer(inv -> {
                BasePermission arg = inv.getArgument(0);
                return new BasePermission(BasePermissionMother.BASE_PERMISSION_ID, arg.getName(),
                        arg.getCode(), arg.getSubModule(), arg.getCreatedDate(), arg.isEnabled());
            });

            BasePermissionDto dto = service.execute(BasePermissionMother.comandoCrear());

            ArgumentCaptor<BasePermission> captor = ArgumentCaptor.forClass(BasePermission.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Crear factura");
            assertThat(captor.getValue().getCode()).isEqualTo("INVOICE_CREATE");
            assertThat(captor.getValue().getSubModule()).isEqualTo(BasePermissionMother.VENTAS);
            assertThat(dto.id()).isEqualTo(BasePermissionMother.BASE_PERMISSION_ID);

            verify(mandatoryBaseRolePermissionInitializationPort)
                    .initializeForMandatoryBaseRoles(BasePermissionMother.BASE_PERMISSION_ID);
            verify(adminPermissionPublisher).publish();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca el repositorio ni inicializa roles si el submodulo no existe")
        void no_toca_nada_si_el_submodulo_no_existe() {
            when(subModuleQueryPort.findById(BasePermissionMother.VENTAS.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BasePermissionMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "SubModule not found: " + BasePermissionMother.VENTAS.id());

            verifyNoInteractions(repository, mandatoryBaseRolePermissionInitializationPort,
                    adminPermissionPublisher);
        }
    }
}
