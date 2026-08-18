package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa.
 *
 * <p>
 * {@code BaseRoleJpaEntity} y {@code BasePermissionJpaEntity} se mockean porque
 * sus constructores sin argumentos son {@code protected} y no son instanciables
 * desde este paquete. No tienen logica: son portadores de datos, y mockearlos
 * no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseRolePermissionJpaMapper — ida y vuelta dominio <-> entidad JPA")
class BaseRolePermissionJpaMapperTest {

    private final BaseRolePermissionJpaMapper mapper = new BaseRolePermissionJpaMapper();

    @Mock
    private BaseRoleJpaEntity baseRoleEntity;
    @Mock
    private BasePermissionJpaEntity basePermissionEntity;

    @Test
    @DisplayName("toJpa copia cada campo del dominio, incluidos el rol y el permiso dados")
    void to_jpa_copia_cada_campo_del_dominio() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BaseRolePermission vinculo = new BaseRolePermission(2L,
                new BaseRoleRef(1L, "Veterinario", "VET"),
                new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE"), creado, true);

        BaseRolePermissionJpaEntity entity = mapper.toJpa(vinculo, baseRoleEntity,
                basePermissionEntity);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getBaseRole()).isSameAs(baseRoleEntity);
        assertThat(entity.getBasePermission()).isSameAs(basePermissionEntity);
        assertThat(entity.getCreatedDate()).isEqualTo(creado);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toJpa conserva enabled=false de un vinculo deshabilitado")
    void to_jpa_conserva_enabled_false() {
        BaseRolePermission vinculo = new BaseRolePermission(2L,
                new BaseRoleRef(1L, "Veterinario", "VET"),
                new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE"),
                LocalDateTime.of(2026, 1, 15, 10, 30), false);

        BaseRolePermissionJpaEntity entity = mapper.toJpa(vinculo, baseRoleEntity,
                basePermissionEntity);

        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity) reconstruye el rol y el permiso desde las relaciones JPA")
    void to_domain_reconstruye_el_rol_y_el_permiso_desde_las_relaciones() {
        when(baseRoleEntity.getId()).thenReturn(1L);
        when(baseRoleEntity.getName()).thenReturn("Veterinario");
        when(baseRoleEntity.getCode()).thenReturn("VET");
        when(basePermissionEntity.getId()).thenReturn(10L);
        when(basePermissionEntity.getName()).thenReturn("Crear consulta");
        when(basePermissionEntity.getCode()).thenReturn("CONSULTA_CREATE");
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BaseRolePermissionJpaEntity entity = new BaseRolePermissionJpaEntity();
        entity.setId(2L);
        entity.setBaseRole(baseRoleEntity);
        entity.setBasePermission(basePermissionEntity);
        entity.setCreatedDate(creado);
        entity.setEnabled(false);

        BaseRolePermission vinculo = mapper.toDomain(entity);

        assertThat(vinculo.getId()).isEqualTo(2L);
        assertThat(vinculo.getBaseRole()).isEqualTo(new BaseRoleRef(1L, "Veterinario", "VET"));
        assertThat(vinculo.getBasePermission())
                .isEqualTo(new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE"));
        assertThat(vinculo.getCreatedDate()).isEqualTo(creado);
        assertThat(vinculo.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity, baseRoleRef, basePermissionRef) usa las referencias dadas sin tocar las relaciones JPA")
    void to_domain_con_refs_usa_las_referencias_dadas() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BaseRolePermissionJpaEntity entity = new BaseRolePermissionJpaEntity();
        entity.setId(2L);
        entity.setCreatedDate(creado);
        entity.setEnabled(true);
        BaseRoleRef baseRoleRef = new BaseRoleRef(9L, "Administrador", "ADMIN");
        BasePermissionRef basePermissionRef = new BasePermissionRef(19L, "Editar consulta",
                "CONSULTA_UPDATE");

        BaseRolePermission vinculo = mapper.toDomain(entity, baseRoleRef, basePermissionRef);

        assertThat(vinculo.getBaseRole()).isEqualTo(baseRoleRef);
        assertThat(vinculo.getBasePermission()).isEqualTo(basePermissionRef);
        assertThat(vinculo.getId()).isEqualTo(2L);
    }
}
