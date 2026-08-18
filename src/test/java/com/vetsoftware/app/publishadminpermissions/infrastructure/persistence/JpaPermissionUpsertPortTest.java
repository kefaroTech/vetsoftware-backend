package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaEntity;
import com.vetsoftware.app.permission.infrastructure.persistence.PermissionJpaRepository;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermission;
import com.vetsoftware.app.publishadminpermissions.application.port.out.UpsertedPermission;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
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
 * {@code CompanyJpaEntity} y {@code SubModuleJpaEntity} se mockean porque sus
 * constructores sin argumentos son {@code protected} y no son instanciables
 * desde este paquete; aqui solo actuan como referencias devueltas por
 * {@code getReferenceById}. {@code PermissionJpaEntity} tiene constructor
 * publico y se instancia real: es la entidad que arma el adaptador.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPermissionUpsertPort — publicacion idempotente del permiso por empresa")
class JpaPermissionUpsertPortTest {

    private static final AdminBasePermission PLANTILLA = new AdminBasePermission(101L,
            "animal.read", "Ver animales", 5L);

    @Mock
    private PermissionJpaRepository permissionJpaRepository;
    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private SubModuleJpaRepository subModuleJpaRepository;
    @Mock
    private PermissionJpaEntity permisoExistente;
    @Mock
    private CompanyJpaEntity empresaRef;
    @Mock
    private SubModuleJpaEntity subModuloRef;
    @InjectMocks
    private JpaPermissionUpsertPort port;

    @Nested
    @DisplayName("permiso ya publicado")
    class PermisoYaPublicado {

        @Test
        @DisplayName("no crea un duplicado y devuelve el id existente")
        void no_crea_duplicado_y_devuelve_el_id_existente() {
            when(permisoExistente.getId()).thenReturn(900L);
            when(permissionJpaRepository.findByCompanyIdAndCode(1L, "animal.read"))
                    .thenReturn(Optional.of(permisoExistente));

            UpsertedPermission resultado = port.upsert(1L, PLANTILLA);

            assertThat(resultado).isEqualTo(new UpsertedPermission(900L, false));
            verifyNoInteractions(companyJpaRepository, subModuleJpaRepository);
        }
    }

    @Nested
    @DisplayName("permiso nuevo")
    class PermisoNuevo {

        @Test
        @DisplayName("crea el permiso con los datos de la plantilla y lo guarda")
        void crea_el_permiso_con_los_datos_de_la_plantilla() {
            when(permissionJpaRepository.findByCompanyIdAndCode(1L, "animal.read"))
                    .thenReturn(Optional.empty());
            when(companyJpaRepository.getReferenceById(1L)).thenReturn(empresaRef);
            when(subModuleJpaRepository.getReferenceById(5L)).thenReturn(subModuloRef);
            when(permissionJpaRepository.save(any())).thenAnswer(inv -> {
                PermissionJpaEntity guardado = inv.getArgument(0);
                guardado.setId(77L);
                return guardado;
            });

            UpsertedPermission resultado = port.upsert(1L, PLANTILLA);

            ArgumentCaptor<PermissionJpaEntity> captor = ArgumentCaptor
                    .forClass(PermissionJpaEntity.class);
            verify(permissionJpaRepository).save(captor.capture());
            PermissionJpaEntity guardado = captor.getValue();
            assertThat(guardado.getName()).isEqualTo("Ver animales");
            assertThat(guardado.getCode()).isEqualTo("animal.read");
            assertThat(guardado.getCompany()).isEqualTo(empresaRef);
            assertThat(guardado.getSubModule()).isEqualTo(subModuloRef);
            assertThat(guardado.getCreatedDate()).isNotNull();
            assertThat(resultado).isEqualTo(new UpsertedPermission(77L, true));
        }
    }
}
