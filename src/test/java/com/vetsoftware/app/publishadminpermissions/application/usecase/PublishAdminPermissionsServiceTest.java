package com.vetsoftware.app.publishadminpermissions.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.publishadminpermissions.application.dto.PublishAdminPermissionsDto;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermission;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermissionsQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBaseRoleQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyAdminContext;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyCatalogQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyGrantedSubModuleIdsQueryPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.PermissionUpsertPort;
import com.vetsoftware.app.publishadminpermissions.application.port.out.RolePermissionUpsertPort;
import com.vetsoftware.app.publishadminpermissions.testsupport.PublishAdminPermissionsMother;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishAdminPermissionsService — sincronizacion de permisos del rol ADMIN")
class PublishAdminPermissionsServiceTest {

    private static final Long ADMIN_BASE_ROLE_ID = 1L;

    @Mock
    private AdminBaseRoleQueryPort adminBaseRoleQueryPort;
    @Mock
    private AdminBasePermissionsQueryPort adminBasePermissionsQueryPort;
    @Mock
    private CompanyCatalogQueryPort companyCatalogQueryPort;
    @Mock
    private CompanyGrantedSubModuleIdsQueryPort companyGrantedSubModuleIdsQueryPort;
    @Mock
    private PermissionUpsertPort permissionUpsertPort;
    @Mock
    private RolePermissionUpsertPort rolePermissionUpsertPort;
    @InjectMocks
    private PublishAdminPermissionsService service;

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("sin rol base ADMIN configurado lanza IllegalStateException y no toca el resto de puertos")
        void sin_rol_base_admin_configurado_lanza_illegal_state_exception() {
            when(adminBaseRoleQueryPort.findAdminBaseRoleId()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute()).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("BaseRole 'ADMIN' not configured");

            verifyNoInteractions(adminBasePermissionsQueryPort, companyCatalogQueryPort,
                    companyGrantedSubModuleIdsQueryPort, permissionUpsertPort,
                    rolePermissionUpsertPort);
        }
    }

    @Nested
    @DisplayName("agrupamientos vacios")
    class AgrupamientosVacios {

        @Test
        @DisplayName("sin permisos base que publicar no escribe nada y reporta las empresas sin cambios")
        void sin_permisos_base_no_escribe_nada() {
            when(adminBaseRoleQueryPort.findAdminBaseRoleId())
                    .thenReturn(Optional.of(ADMIN_BASE_ROLE_ID));
            when(adminBasePermissionsQueryPort.findByAdminBaseRoleId(ADMIN_BASE_ROLE_ID))
                    .thenReturn(List.of());
            when(companyCatalogQueryPort.findAllWithAdminRole())
                    .thenReturn(List.of(PublishAdminPermissionsMother.clinicaNorte(),
                            PublishAdminPermissionsMother.clinicaSur()));

            PublishAdminPermissionsDto dto = service.execute();

            assertThat(dto).isEqualTo(new PublishAdminPermissionsDto(2, 0, 0, 0));
            verifyNoInteractions(companyGrantedSubModuleIdsQueryPort, permissionUpsertPort,
                    rolePermissionUpsertPort);
        }

        @Test
        @DisplayName("sin empresas con rol ADMIN no escribe nada")
        void sin_empresas_con_rol_admin_no_escribe_nada() {
            when(adminBaseRoleQueryPort.findAdminBaseRoleId())
                    .thenReturn(Optional.of(ADMIN_BASE_ROLE_ID));
            when(adminBasePermissionsQueryPort.findByAdminBaseRoleId(ADMIN_BASE_ROLE_ID))
                    .thenReturn(List.of(PublishAdminPermissionsMother.verAnimales()));
            when(companyCatalogQueryPort.findAllWithAdminRole()).thenReturn(List.of());

            PublishAdminPermissionsDto dto = service.execute();

            assertThat(dto).isEqualTo(new PublishAdminPermissionsDto(0, 0, 0, 0));
            verifyNoInteractions(companyGrantedSubModuleIdsQueryPort, permissionUpsertPort,
                    rolePermissionUpsertPort);
        }

        @Test
        @DisplayName("una empresa cuyo contrato no concede el submodulo no publica el permiso")
        void empresa_sin_el_submodulo_habilitado_no_publica_el_permiso() {
            CompanyAdminContext clinica = PublishAdminPermissionsMother.clinicaNorte();
            AdminBasePermission plantilla = PublishAdminPermissionsMother.verAnimales();
            when(adminBaseRoleQueryPort.findAdminBaseRoleId())
                    .thenReturn(Optional.of(ADMIN_BASE_ROLE_ID));
            when(adminBasePermissionsQueryPort.findByAdminBaseRoleId(ADMIN_BASE_ROLE_ID))
                    .thenReturn(List.of(plantilla));
            when(companyCatalogQueryPort.findAllWithAdminRole()).thenReturn(List.of(clinica));
            when(companyGrantedSubModuleIdsQueryPort
                    .findGrantedSubModuleIdsByCompanyIds(Set.of(1L))).thenReturn(Map.of());

            PublishAdminPermissionsDto dto = service.execute();

            assertThat(dto).isEqualTo(new PublishAdminPermissionsDto(1, 0, 0, 0));
            verifyNoInteractions(permissionUpsertPort, rolePermissionUpsertPort);
        }
    }

    @Nested
    @DisplayName("publicacion")
    class Publicacion {

        @Test
        @DisplayName("publica el permiso nuevo y lo vincula al rol admin de la empresa")
        void publica_el_permiso_nuevo_y_lo_vincula() {
            CompanyAdminContext clinica = PublishAdminPermissionsMother.clinicaNorte();
            AdminBasePermission plantilla = PublishAdminPermissionsMother.verAnimales();
            when(adminBaseRoleQueryPort.findAdminBaseRoleId())
                    .thenReturn(Optional.of(ADMIN_BASE_ROLE_ID));
            when(adminBasePermissionsQueryPort.findByAdminBaseRoleId(ADMIN_BASE_ROLE_ID))
                    .thenReturn(List.of(plantilla));
            when(companyCatalogQueryPort.findAllWithAdminRole()).thenReturn(List.of(clinica));
            when(companyGrantedSubModuleIdsQueryPort
                    .findGrantedSubModuleIdsByCompanyIds(Set.of(1L)))
                    .thenReturn(Map.of(1L, Set.of(5L)));
            when(permissionUpsertPort.upsert(1L, plantilla))
                    .thenReturn(PublishAdminPermissionsMother.creado(900L));
            when(rolePermissionUpsertPort.linkIfAbsent(100L, 900L)).thenReturn(true);

            PublishAdminPermissionsDto dto = service.execute();

            ArgumentCaptor<Long> companyIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<AdminBasePermission> plantillaCaptor = ArgumentCaptor
                    .forClass(AdminBasePermission.class);
            verify(permissionUpsertPort).upsert(companyIdCaptor.capture(),
                    plantillaCaptor.capture());
            assertThat(companyIdCaptor.getValue()).isEqualTo(1L);
            assertThat(plantillaCaptor.getValue()).isEqualTo(plantilla);
            verify(rolePermissionUpsertPort).linkIfAbsent(100L, 900L);
            assertThat(dto).isEqualTo(new PublishAdminPermissionsDto(1, 1, 1, 1));
        }

        @Test
        @DisplayName("no duplica el permiso si ya existe pero si crea el vinculo con el rol")
        void no_duplica_el_permiso_pero_crea_el_vinculo() {
            CompanyAdminContext clinica = PublishAdminPermissionsMother.clinicaNorte();
            AdminBasePermission plantilla = PublishAdminPermissionsMother.verAnimales();
            when(adminBaseRoleQueryPort.findAdminBaseRoleId())
                    .thenReturn(Optional.of(ADMIN_BASE_ROLE_ID));
            when(adminBasePermissionsQueryPort.findByAdminBaseRoleId(ADMIN_BASE_ROLE_ID))
                    .thenReturn(List.of(plantilla));
            when(companyCatalogQueryPort.findAllWithAdminRole()).thenReturn(List.of(clinica));
            when(companyGrantedSubModuleIdsQueryPort
                    .findGrantedSubModuleIdsByCompanyIds(Set.of(1L)))
                    .thenReturn(Map.of(1L, Set.of(5L)));
            when(permissionUpsertPort.upsert(1L, plantilla))
                    .thenReturn(PublishAdminPermissionsMother.existente(900L));
            when(rolePermissionUpsertPort.linkIfAbsent(100L, 900L)).thenReturn(true);

            PublishAdminPermissionsDto dto = service.execute();

            assertThat(dto).isEqualTo(new PublishAdminPermissionsDto(1, 1, 0, 1));
        }

        @Test
        @DisplayName("no cuenta la empresa como actualizada si el permiso y el vinculo ya existian")
        void no_cuenta_la_empresa_si_todo_ya_existia() {
            CompanyAdminContext clinica = PublishAdminPermissionsMother.clinicaNorte();
            AdminBasePermission plantilla = PublishAdminPermissionsMother.verAnimales();
            when(adminBaseRoleQueryPort.findAdminBaseRoleId())
                    .thenReturn(Optional.of(ADMIN_BASE_ROLE_ID));
            when(adminBasePermissionsQueryPort.findByAdminBaseRoleId(ADMIN_BASE_ROLE_ID))
                    .thenReturn(List.of(plantilla));
            when(companyCatalogQueryPort.findAllWithAdminRole()).thenReturn(List.of(clinica));
            when(companyGrantedSubModuleIdsQueryPort
                    .findGrantedSubModuleIdsByCompanyIds(Set.of(1L)))
                    .thenReturn(Map.of(1L, Set.of(5L)));
            when(permissionUpsertPort.upsert(1L, plantilla))
                    .thenReturn(PublishAdminPermissionsMother.existente(900L));
            when(rolePermissionUpsertPort.linkIfAbsent(100L, 900L)).thenReturn(false);

            PublishAdminPermissionsDto dto = service.execute();

            assertThat(dto).isEqualTo(new PublishAdminPermissionsDto(1, 0, 0, 0));
        }

        @Test
        @DisplayName("cuenta solo las empresas que realmente cambiaron cuando hay varias")
        void cuenta_solo_las_empresas_que_realmente_cambiaron() {
            CompanyAdminContext clinicaConCambios = PublishAdminPermissionsMother.clinicaNorte();
            CompanyAdminContext clinicaSinConcesion = PublishAdminPermissionsMother.clinicaSur();
            AdminBasePermission plantilla = PublishAdminPermissionsMother.verAnimales();
            when(adminBaseRoleQueryPort.findAdminBaseRoleId())
                    .thenReturn(Optional.of(ADMIN_BASE_ROLE_ID));
            when(adminBasePermissionsQueryPort.findByAdminBaseRoleId(ADMIN_BASE_ROLE_ID))
                    .thenReturn(List.of(plantilla));
            when(companyCatalogQueryPort.findAllWithAdminRole())
                    .thenReturn(List.of(clinicaConCambios, clinicaSinConcesion));
            when(companyGrantedSubModuleIdsQueryPort
                    .findGrantedSubModuleIdsByCompanyIds(Set.of(1L, 2L)))
                    .thenReturn(Map.of(1L, Set.of(5L)));
            when(permissionUpsertPort.upsert(1L, plantilla))
                    .thenReturn(PublishAdminPermissionsMother.creado(900L));
            when(rolePermissionUpsertPort.linkIfAbsent(100L, 900L)).thenReturn(true);

            PublishAdminPermissionsDto dto = service.execute();

            assertThat(dto).isEqualTo(new PublishAdminPermissionsDto(2, 1, 1, 1));
            verify(permissionUpsertPort).upsert(1L, plantilla);
            verify(rolePermissionUpsertPort).linkIfAbsent(100L, 900L);
        }
    }
}
