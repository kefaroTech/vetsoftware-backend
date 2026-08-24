package com.vetsoftware.app.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.entitlement.infrastructure.persistence.CompanyEntitlementJpaRepository;
import com.vetsoftware.app.entitlement.infrastructure.persistence.CompanyEntitlementJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaEntity;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Instancia (y enlaza al rol) los permisos de la empresa a partir de los
 * permisos base, filtrados por los submodulos que el <b>contrato</b> de la
 * empresa le concede ({@code company_entitlements} en nivel {@code FULL} o
 * {@code READ_ONLY}). Toca seis repositorios de Spring Data de otras features;
 * se prueba con Mockito puro y las entidades JPA con constructor protegido
 * (Company/Role/SubModule/BasePermission) se doblan igual que un
 * {@code getReferenceById()} sin hidratar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaRolePermissionInitializationPort")
class JpaRolePermissionInitializationPortTest {

    private static final Long ROLE_ID = 100L;
    private static final Long COMPANY_ID = 9L;
    private static final Long BASE_ROLE_ID = 1L;
    private static final Long SUB_MODULE_ID = 5L;
    private static final List<String> NIVELES_CONCEDIDOS = List.of("FULL", "READ_ONLY");

    @Mock
    private CompanyEntitlementJpaRepository companyEntitlementJpaRepository;
    @Mock
    private BaseRolePermissionJpaRepository baseRolePermissionJpaRepository;
    @Mock
    private PermissionJpaRepository permissionJpaRepository;
    @Mock
    private RolePermissionJpaRepository rolePermissionJpaRepository;
    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private RoleJpaRepository roleJpaRepository;
    @Mock
    private BaseRoleJpaRepository baseRoleJpaRepository;
    @Mock
    private PermissionCachePort permissionCachePort;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:30:00Z"), ZoneOffset.UTC);
    @InjectMocks
    private JpaRolePermissionInitializationPort port;

    private void conceden(Long... subModuleIds) {
        when(companyEntitlementJpaRepository.findGrantedSubModuleIdsByCompanyId(COMPANY_ID,
                NIVELES_CONCEDIDOS)).thenReturn(List.of(subModuleIds));
    }

    private static BaseRolePermissionJpaEntity permisoBase(String code, String name,
            Long subModuleId) {
        SubModuleJpaEntity subModule = mock(SubModuleJpaEntity.class);
        when(subModule.getId()).thenReturn(subModuleId);
        BasePermissionJpaEntity basePermission = mock(BasePermissionJpaEntity.class);
        // lenient: fixture compartida entre escenarios. getCode()/getName() solo se
        // leen cuando el permiso sobrevive el filtro de submodulo y aun no existe en la
        // empresa (orElseGet); el escenario "filtrado" y el "ya existe" nunca llegan a
        // consumirlos.
        org.mockito.Mockito.lenient().when(basePermission.getCode()).thenReturn(code);
        org.mockito.Mockito.lenient().when(basePermission.getName()).thenReturn(name);
        when(basePermission.getSubModule()).thenReturn(subModule);
        BaseRolePermissionJpaEntity link = new BaseRolePermissionJpaEntity();
        link.setBasePermission(basePermission);
        return link;
    }

    @Nested
    @DisplayName("Empresa sin nada concedido")
    class SinConcesiones {

        /**
         * Antes esto era un {@code return} mudo y la empresa nacia sin un solo permiso,
         * sin ninguna señal. Con el modelo nuevo es imposible por construccion —toda
         * empresa nace con contrato— asi que llegar aqui es un fallo de la cadena, y
         * tiene que decirlo con el id de la empresa delante.
         */
        @Test
        @DisplayName("una empresa sin submodulos concedidos falla nombrandola, en vez de callar")
        void empresa_sin_concesiones_falla_nombrandola() {
            conceden();

            assertThatThrownBy(() -> port.initializeForRole(ROLE_ID, COMPANY_ID, BASE_ROLE_ID))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("La empresa 9")
                    .hasMessageContaining("company_entitlements");

            verify(baseRolePermissionJpaRepository, never()).findByBaseRoleId(any());
            verify(permissionJpaRepository, never()).saveAll(any());
            verify(rolePermissionJpaRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Sin permisos base aplicables")
    class SinPermisosAplicables {

        /**
         * Este si es legitimo: un rol base cuyos permisos caen todos fuera de lo
         * concedido queda como plantilla vacia. No escribe nada y no rompe el alta.
         */
        @Test
        @DisplayName("ningun permiso base del rol cae dentro de lo concedido: no escribe nada")
        void sin_permisos_dentro_de_lo_concedido_no_escribe_nada() {
            // Los mocks se resuelven en variables antes de stubear: construirlos dentro
            // del propio thenReturn() intercalaria sus when(...) internos con este
            // when(...) todavia abierto.
            BaseRolePermissionJpaEntity otroPermiso = permisoBase("OTRO.PERMISO", "Otro", 999L);
            conceden(SUB_MODULE_ID);
            when(baseRolePermissionJpaRepository.findByBaseRoleId(BASE_ROLE_ID))
                    .thenReturn(List.of(otroPermiso));

            port.initializeForRole(ROLE_ID, COMPANY_ID, BASE_ROLE_ID);

            verify(companyJpaRepository, never()).getReferenceById(any());
            verify(permissionJpaRepository, never()).saveAll(any());
            verify(rolePermissionJpaRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Inicializacion de permisos y vinculos")
    class Inicializacion {

        @Test
        @DisplayName("crea el permiso de la empresa cuando aun no existe y lo enlaza al rol")
        void crea_el_permiso_que_falta_y_lo_enlaza_al_rol() {
            CompanyJpaEntity companyRef = mock(CompanyJpaEntity.class);
            RoleJpaEntity roleRef = mock(RoleJpaEntity.class);
            BaseRolePermissionJpaEntity permisoAnimalCreate = permisoBase("ANIMAL.CREATE",
                    "Crear animal", SUB_MODULE_ID);
            conceden(SUB_MODULE_ID);
            when(baseRolePermissionJpaRepository.findByBaseRoleId(BASE_ROLE_ID))
                    .thenReturn(List.of(permisoAnimalCreate));
            when(companyJpaRepository.getReferenceById(COMPANY_ID)).thenReturn(companyRef);
            when(permissionJpaRepository.findByCompanyIdAndCode(COMPANY_ID, "ANIMAL.CREATE"))
                    .thenReturn(Optional.empty());
            when(roleJpaRepository.getReferenceById(ROLE_ID)).thenReturn(roleRef);

            port.initializeForRole(ROLE_ID, COMPANY_ID, BASE_ROLE_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<PermissionJpaEntity>> permisosCreados = ArgumentCaptor
                    .forClass(List.class);
            verify(permissionJpaRepository).saveAll(permisosCreados.capture());
            assertThat(permisosCreados.getValue()).hasSize(1);
            PermissionJpaEntity creado = permisosCreados.getValue().get(0);
            assertThat(creado.getName()).isEqualTo("Crear animal");
            assertThat(creado.getCode()).isEqualTo("ANIMAL.CREATE");
            assertThat(creado.getCompany()).isSameAs(companyRef);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RolePermissionJpaEntity>> vinculos = ArgumentCaptor
                    .forClass(List.class);
            verify(rolePermissionJpaRepository).saveAll(vinculos.capture());
            assertThat(vinculos.getValue()).hasSize(1);
            assertThat(vinculos.getValue().get(0).getRole()).isSameAs(roleRef);
            assertThat(vinculos.getValue().get(0).getPermission()).isEqualTo(creado);
        }

        @Test
        @DisplayName("reutiliza el permiso que ya existe en la empresa sin volver a crearlo")
        void reutiliza_el_permiso_existente_sin_recrearlo() {
            CompanyJpaEntity companyRef = mock(CompanyJpaEntity.class);
            RoleJpaEntity roleRef = mock(RoleJpaEntity.class);
            PermissionJpaEntity existente = new PermissionJpaEntity();
            BaseRolePermissionJpaEntity permisoAnimalCreate = permisoBase("ANIMAL.CREATE",
                    "Crear animal", SUB_MODULE_ID);
            conceden(SUB_MODULE_ID);
            when(baseRolePermissionJpaRepository.findByBaseRoleId(BASE_ROLE_ID))
                    .thenReturn(List.of(permisoAnimalCreate));
            when(companyJpaRepository.getReferenceById(COMPANY_ID)).thenReturn(companyRef);
            when(permissionJpaRepository.findByCompanyIdAndCode(COMPANY_ID, "ANIMAL.CREATE"))
                    .thenReturn(Optional.of(existente));
            when(roleJpaRepository.getReferenceById(ROLE_ID)).thenReturn(roleRef);

            port.initializeForRole(ROLE_ID, COMPANY_ID, BASE_ROLE_ID);

            verify(permissionJpaRepository, never()).saveAll(any());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RolePermissionJpaEntity>> vinculos = ArgumentCaptor
                    .forClass(List.class);
            verify(rolePermissionJpaRepository).saveAll(vinculos.capture());
            assertThat(vinculos.getValue()).hasSize(1);
            assertThat(vinculos.getValue().get(0).getPermission()).isSameAs(existente);
        }
    }

    @Nested
    @DisplayName("Reconciliacion del rol ADMIN")
    class ReconciliacionAdmin {

        private static final LocalDateTime RECALCULADO = LocalDateTime.of(2026, 8, 23, 12, 0);

        private RoleJpaEntity admin() {
            RoleJpaEntity role = mock(RoleJpaEntity.class);
            when(role.getId()).thenReturn(ROLE_ID);
            return role;
        }

        private BaseRoleJpaEntity baseAdmin() {
            BaseRoleJpaEntity role = mock(BaseRoleJpaEntity.class);
            when(role.getId()).thenReturn(BASE_ROLE_ID);
            return role;
        }

        private CompanyEntitlementJpaEntity entitlement(String accessLevel) {
            CompanyEntitlementJpaEntity entitlement = mock(CompanyEntitlementJpaEntity.class);
            SubModuleJpaEntity subModule = mock(SubModuleJpaEntity.class);
            when(subModule.getId()).thenReturn(SUB_MODULE_ID);
            when(entitlement.getSubModule()).thenReturn(subModule);
            when(entitlement.getAccessLevel()).thenReturn(accessLevel);
            when(entitlement.getValidFrom()).thenReturn(RECALCULADO.minusDays(1));
            when(entitlement.getValidUntil()).thenReturn(null);
            return entitlement;
        }

        private void base(RoleJpaEntity admin, BasePermissionJpaEntity template,
                String accessLevel) {
            BaseRolePermissionJpaEntity link = new BaseRolePermissionJpaEntity();
            link.setBasePermission(template);
            BaseRoleJpaEntity baseRole = baseAdmin();
            CompanyEntitlementJpaEntity effectiveEntitlement = entitlement(accessLevel);
            when(roleJpaRepository.findByCompanyIdAndCode(COMPANY_ID, "ADMIN"))
                    .thenReturn(Optional.of(admin));
            when(baseRoleJpaRepository.findByCode("ADMIN")).thenReturn(Optional.of(baseRole));
            when(baseRolePermissionJpaRepository.findByBaseRoleId(BASE_ROLE_ID))
                    .thenReturn(List.of(link));
            when(companyEntitlementJpaRepository.findAllByCompany_Id(COMPANY_ID))
                    .thenReturn(List.of(effectiveEntitlement));
        }

        @Test
        @DisplayName("crea permiso y vinculo faltantes y entonces invalida la cache del ADMIN")
        void crea_permiso_y_vinculo_e_invalida_cache() {
            RoleJpaEntity admin = admin();
            BasePermissionJpaEntity template = permisoBase("animal.read", "Leer animales",
                    SUB_MODULE_ID).getBasePermission();
            base(admin, template, "FULL");
            when(permissionJpaRepository.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of());
            when(permissionJpaRepository.findDisabledIdByCompanyIdAndCode(COMPANY_ID,
                    "animal.read")).thenReturn(Optional.empty());
            when(companyJpaRepository.getReferenceById(COMPANY_ID))
                    .thenReturn(mock(CompanyJpaEntity.class));
            when(permissionJpaRepository.save(any())).thenAnswer(invocation -> {
                PermissionJpaEntity permission = invocation.getArgument(0);
                permission.setId(200L);
                return permission;
            });
            when(rolePermissionJpaRepository.findAllByRoleId(ROLE_ID)).thenReturn(List.of());
            when(roleJpaRepository.getReferenceById(ROLE_ID)).thenReturn(admin);
            when(rolePermissionJpaRepository.findDisabledIdByRoleAndPermission(ROLE_ID, 200L))
                    .thenReturn(Optional.empty());

            port.reconcile(COMPANY_ID, RECALCULADO);

            verify(permissionJpaRepository).save(any(PermissionJpaEntity.class));
            verify(rolePermissionJpaRepository).save(any(RolePermissionJpaEntity.class));
            verify(permissionCachePort).evictByRoleId(ROLE_ID);
        }

        @Test
        @DisplayName("reactiva el vinculo ADMIN existente sin duplicarlo e invalida la cache")
        void reactiva_vinculo_existente_e_invalida_cache() {
            RoleJpaEntity admin = admin();
            BasePermissionJpaEntity template = permisoBase("animal.read", "Leer animales",
                    SUB_MODULE_ID).getBasePermission();
            PermissionJpaEntity permission = new PermissionJpaEntity();
            permission.setId(200L);
            permission.setCode("animal.read");
            base(admin, template, "FULL");
            when(permissionJpaRepository.findAllByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(permission));
            when(rolePermissionJpaRepository.findAllByRoleId(ROLE_ID)).thenReturn(List.of());
            when(roleJpaRepository.getReferenceById(ROLE_ID)).thenReturn(admin);
            when(rolePermissionJpaRepository.findDisabledIdByRoleAndPermission(ROLE_ID, 200L))
                    .thenReturn(Optional.of(300L));
            when(rolePermissionJpaRepository.reactivate(300L, COMPANY_ID)).thenReturn(1);

            port.reconcile(COMPANY_ID, RECALCULADO);

            verify(rolePermissionJpaRepository).reactivate(300L, COMPANY_ID);
            verify(rolePermissionJpaRepository, never()).save(any());
            verify(permissionCachePort).evictByRoleId(ROLE_ID);
        }

        @Test
        @DisplayName("READ_ONLY deshabilita la mutacion ADMIN obsoleta e invalida la cache")
        void read_only_deshabilita_mutacion_e_invalida_cache() {
            RoleJpaEntity admin = admin();
            BasePermissionJpaEntity template = permisoBase("animal.update", "Editar animales",
                    SUB_MODULE_ID).getBasePermission();
            PermissionJpaEntity permission = new PermissionJpaEntity();
            permission.setId(200L);
            permission.setCode("animal.update");
            RolePermissionJpaEntity activeLink = new RolePermissionJpaEntity();
            activeLink.setId(301L);
            activeLink.setPermission(permission);
            base(admin, template, "READ_ONLY");
            when(permissionJpaRepository.findAllByCompanyId(COMPANY_ID))
                    .thenReturn(List.of(permission));
            when(rolePermissionJpaRepository.findAllByRoleId(ROLE_ID))
                    .thenReturn(List.of(activeLink));
            when(roleJpaRepository.getReferenceById(ROLE_ID)).thenReturn(admin);

            port.reconcile(COMPANY_ID, RECALCULADO);

            verify(rolePermissionJpaRepository).disableAllByIds(List.of(301L), COMPANY_ID);
            verify(permissionCachePort).evictByRoleId(ROLE_ID);
        }
    }
}
