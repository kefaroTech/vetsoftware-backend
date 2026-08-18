package com.vetsoftware.app.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaEntity;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaEntity;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Instancia (y enlaza al rol) los permisos de la empresa a partir de los
 * permisos base de la membresía. Toca seis repositorios de Spring Data de otras
 * features; se prueba con Mockito puro y las entidades JPA con constructor
 * protegido (Company/Role/SubModule/BasePermission/ MembershipSubModule) se
 * doblan igual que un {@code getReferenceById()} sin hidratar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaRolePermissionInitializationPort")
class JpaRolePermissionInitializationPortTest {

    private static final Long ROLE_ID = 100L;
    private static final Long COMPANY_ID = 9L;
    private static final Long BASE_ROLE_ID = 1L;
    private static final Long MEMBERSHIP_ID = 2L;
    private static final Long SUB_MODULE_ID = 5L;

    @Mock
    private MembershipSubModuleJpaRepository membershipSubModuleJpaRepository;
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
    @InjectMocks
    private JpaRolePermissionInitializationPort port;

    private static MembershipSubModuleJpaEntity vinculoSubModulo(Long subModuleId) {
        SubModuleJpaEntity subModule = mock(SubModuleJpaEntity.class);
        when(subModule.getId()).thenReturn(subModuleId);
        MembershipSubModuleJpaEntity vinculo = mock(MembershipSubModuleJpaEntity.class);
        when(vinculo.getSubModule()).thenReturn(subModule);
        return vinculo;
    }

    private static BaseRolePermissionJpaEntity permisoBase(String code, String name,
            Long subModuleId) {
        SubModuleJpaEntity subModule = mock(SubModuleJpaEntity.class);
        when(subModule.getId()).thenReturn(subModuleId);
        BasePermissionJpaEntity basePermission = mock(BasePermissionJpaEntity.class);
        // lenient: fixture compartida entre escenarios. getCode()/getName() solo se
        // leen cuando el
        // permiso sobrevive el filtro de submódulo y aún no existe en la empresa
        // (orElseGet); el
        // escenario "filtrado" y el "ya existe" nunca llegan a consumirlos.
        org.mockito.Mockito.lenient().when(basePermission.getCode()).thenReturn(code);
        org.mockito.Mockito.lenient().when(basePermission.getName()).thenReturn(name);
        when(basePermission.getSubModule()).thenReturn(subModule);
        BaseRolePermissionJpaEntity link = new BaseRolePermissionJpaEntity();
        link.setBasePermission(basePermission);
        return link;
    }

    @Nested
    @DisplayName("Sin submódulos que inicializar")
    class SinSubModulos {

        @Test
        @DisplayName("una membresía sin submódulos no toca ningún repositorio de escritura")
        void membresia_sin_submodulos_no_escribe_nada() {
            when(membershipSubModuleJpaRepository.findByMembershipId(MEMBERSHIP_ID))
                    .thenReturn(List.of());

            port.initializeForRole(ROLE_ID, COMPANY_ID, BASE_ROLE_ID, MEMBERSHIP_ID);

            verify(baseRolePermissionJpaRepository, never()).findByBaseRoleId(any());
            verify(permissionJpaRepository, never()).saveAll(any());
            verify(rolePermissionJpaRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Sin permisos base aplicables")
    class SinPermisosAplicables {

        @Test
        @DisplayName("ningún permiso base del rol cae dentro de los submódulos de la membresía")
        void sin_permisos_dentro_de_los_submodulos_no_escribe_nada() {
            // Los mocks se resuelven en variables antes de stubear: construirlos dentro del
            // propio
            // thenReturn() intercalaría sus when(...) internos con este when(...) todavía
            // abierto.
            MembershipSubModuleJpaEntity vinculo = vinculoSubModulo(SUB_MODULE_ID);
            BaseRolePermissionJpaEntity otroPermiso = permisoBase("OTRO.PERMISO", "Otro", 999L);
            when(membershipSubModuleJpaRepository.findByMembershipId(MEMBERSHIP_ID))
                    .thenReturn(List.of(vinculo));
            when(baseRolePermissionJpaRepository.findByBaseRoleId(BASE_ROLE_ID))
                    .thenReturn(List.of(otroPermiso));

            port.initializeForRole(ROLE_ID, COMPANY_ID, BASE_ROLE_ID, MEMBERSHIP_ID);

            verify(companyJpaRepository, never()).getReferenceById(any());
            verify(permissionJpaRepository, never()).saveAll(any());
            verify(rolePermissionJpaRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Inicialización de permisos y vínculos")
    class Inicializacion {

        @Test
        @DisplayName("crea el permiso de la empresa cuando aún no existe y lo enlaza al rol")
        void crea_el_permiso_que_falta_y_lo_enlaza_al_rol() {
            CompanyJpaEntity companyRef = mock(CompanyJpaEntity.class);
            RoleJpaEntity roleRef = mock(RoleJpaEntity.class);
            MembershipSubModuleJpaEntity vinculo = vinculoSubModulo(SUB_MODULE_ID);
            BaseRolePermissionJpaEntity permisoAnimalCreate = permisoBase("ANIMAL.CREATE",
                    "Crear animal", SUB_MODULE_ID);
            when(membershipSubModuleJpaRepository.findByMembershipId(MEMBERSHIP_ID))
                    .thenReturn(List.of(vinculo));
            when(baseRolePermissionJpaRepository.findByBaseRoleId(BASE_ROLE_ID))
                    .thenReturn(List.of(permisoAnimalCreate));
            when(companyJpaRepository.getReferenceById(COMPANY_ID)).thenReturn(companyRef);
            when(permissionJpaRepository.findByCompanyIdAndCode(COMPANY_ID, "ANIMAL.CREATE"))
                    .thenReturn(Optional.empty());
            when(roleJpaRepository.getReferenceById(ROLE_ID)).thenReturn(roleRef);

            port.initializeForRole(ROLE_ID, COMPANY_ID, BASE_ROLE_ID, MEMBERSHIP_ID);

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
            MembershipSubModuleJpaEntity vinculo = vinculoSubModulo(SUB_MODULE_ID);
            BaseRolePermissionJpaEntity permisoAnimalCreate = permisoBase("ANIMAL.CREATE",
                    "Crear animal", SUB_MODULE_ID);
            when(membershipSubModuleJpaRepository.findByMembershipId(MEMBERSHIP_ID))
                    .thenReturn(List.of(vinculo));
            when(baseRolePermissionJpaRepository.findByBaseRoleId(BASE_ROLE_ID))
                    .thenReturn(List.of(permisoAnimalCreate));
            when(companyJpaRepository.getReferenceById(COMPANY_ID)).thenReturn(companyRef);
            when(permissionJpaRepository.findByCompanyIdAndCode(COMPANY_ID, "ANIMAL.CREATE"))
                    .thenReturn(Optional.of(existente));
            when(roleJpaRepository.getReferenceById(ROLE_ID)).thenReturn(roleRef);

            port.initializeForRole(ROLE_ID, COMPANY_ID, BASE_ROLE_ID, MEMBERSHIP_ID);

            verify(permissionJpaRepository, never()).saveAll(any());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RolePermissionJpaEntity>> vinculos = ArgumentCaptor
                    .forClass(List.class);
            verify(rolePermissionJpaRepository).saveAll(vinculos.capture());
            assertThat(vinculos.getValue()).hasSize(1);
            assertThat(vinculos.getValue().get(0).getPermission()).isSameAs(existente);
        }
    }
}
