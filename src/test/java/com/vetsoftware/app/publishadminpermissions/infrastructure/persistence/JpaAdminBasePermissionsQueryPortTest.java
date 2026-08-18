package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermission;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Las entidades JPA se mockean porque sus constructores sin argumentos son
 * {@code protected} y no son instanciables desde este paquete. No tienen logica
 * propia: son portadoras de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAdminBasePermissionsQueryPort — permisos base del rol ADMIN")
class JpaAdminBasePermissionsQueryPortTest {

    @Mock
    private BaseRolePermissionJpaRepository baseRolePermissionJpaRepository;
    @Mock
    private BaseRolePermissionJpaEntity vinculoUno;
    @Mock
    private BasePermissionJpaEntity permisoBaseUno;
    @Mock
    private SubModuleJpaEntity subModuloUno;
    @InjectMocks
    private JpaAdminBasePermissionsQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea cada vinculo del rol admin a su permiso base con el submodulo")
        void mapea_cada_vinculo_a_su_permiso_base() {
            when(permisoBaseUno.getId()).thenReturn(101L);
            when(permisoBaseUno.getCode()).thenReturn("animal.read");
            when(permisoBaseUno.getName()).thenReturn("Ver animales");
            when(permisoBaseUno.getSubModule()).thenReturn(subModuloUno);
            when(subModuloUno.getId()).thenReturn(5L);
            when(vinculoUno.getBasePermission()).thenReturn(permisoBaseUno);
            when(baseRolePermissionJpaRepository.findByBaseRoleId(1L))
                    .thenReturn(List.of(vinculoUno));

            List<AdminBasePermission> resultado = port.findByAdminBaseRoleId(1L);

            assertThat(resultado).containsExactly(
                    new AdminBasePermission(101L, "animal.read", "Ver animales", 5L));
        }

        @Test
        @DisplayName("un rol admin sin permisos base devuelve lista vacia")
        void sin_permisos_base_devuelve_lista_vacia() {
            when(baseRolePermissionJpaRepository.findByBaseRoleId(1L)).thenReturn(List.of());

            assertThat(port.findByAdminBaseRoleId(1L)).isEmpty();
        }
    }
}
