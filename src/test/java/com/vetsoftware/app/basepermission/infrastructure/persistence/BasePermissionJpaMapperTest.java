package com.vetsoftware.app.basepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
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
 * {@code SubModuleJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es un portador de datos, y mockearlo no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BasePermissionJpaMapper — ida y vuelta dominio <-> entidad JPA")
class BasePermissionJpaMapperTest {

    private final BasePermissionJpaMapper mapper = new BasePermissionJpaMapper();

    @Mock
    private SubModuleJpaEntity subModuleEntity;

    @Test
    @DisplayName("toJpa copia cada campo del dominio, incluido el submodulo dado")
    void to_jpa_copia_cada_campo_del_dominio() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BasePermission basePermission = new BasePermission(2L, "Crear factura", "INVOICE_CREATE",
                new SubModuleRef(1L, "Ventas", "VEN"), creado, true);

        BasePermissionJpaEntity entity = mapper.toJpa(basePermission, subModuleEntity);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getName()).isEqualTo("Crear factura");
        assertThat(entity.getCode()).isEqualTo("INVOICE_CREATE");
        assertThat(entity.getSubModule()).isSameAs(subModuleEntity);
        assertThat(entity.getCreatedDate()).isEqualTo(creado);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toJpa conserva enabled=false de un permiso base deshabilitado")
    void to_jpa_conserva_enabled_false() {
        BasePermission basePermission = new BasePermission(2L, "Crear factura", "INVOICE_CREATE",
                new SubModuleRef(1L, "Ventas", "VEN"), LocalDateTime.of(2026, 1, 15, 10, 30),
                false);

        BasePermissionJpaEntity entity = mapper.toJpa(basePermission, subModuleEntity);

        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity) reconstruye el submodulo desde la relacion JPA")
    void to_domain_reconstruye_el_submodulo_desde_la_relacion() {
        when(subModuleEntity.getId()).thenReturn(1L);
        when(subModuleEntity.getName()).thenReturn("Ventas");
        when(subModuleEntity.getCode()).thenReturn("VEN");
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BasePermissionJpaEntity entity = new BasePermissionJpaEntity();
        entity.setId(2L);
        entity.setName("Crear factura");
        entity.setCode("INVOICE_CREATE");
        entity.setSubModule(subModuleEntity);
        entity.setCreatedDate(creado);
        entity.setEnabled(false);

        BasePermission basePermission = mapper.toDomain(entity);

        assertThat(basePermission.getId()).isEqualTo(2L);
        assertThat(basePermission.getName()).isEqualTo("Crear factura");
        assertThat(basePermission.getCode()).isEqualTo("INVOICE_CREATE");
        assertThat(basePermission.getSubModule()).isEqualTo(new SubModuleRef(1L, "Ventas", "VEN"));
        assertThat(basePermission.getCreatedDate()).isEqualTo(creado);
        assertThat(basePermission.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity, ref) usa la referencia dada sin tocar la relacion JPA")
    void to_domain_con_ref_usa_la_referencia_dada() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        BasePermissionJpaEntity entity = new BasePermissionJpaEntity();
        entity.setId(2L);
        entity.setName("Crear factura");
        entity.setCode("INVOICE_CREATE");
        entity.setCreatedDate(creado);
        entity.setEnabled(true);
        SubModuleRef ref = new SubModuleRef(9L, "Inventario", "INV");

        BasePermission basePermission = mapper.toDomain(entity, ref);

        assertThat(basePermission.getSubModule()).isEqualTo(ref);
        assertThat(basePermission.getName()).isEqualTo("Crear factura");
        assertThat(basePermission.getCode()).isEqualTo("INVOICE_CREATE");
    }
}
