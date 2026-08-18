package com.vetsoftware.app.submodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import com.vetsoftware.app.submodule.domain.ModuleRef;
import com.vetsoftware.app.submodule.domain.SubModule;
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
 * {@code ModuleJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es un portador de datos, y mockearlo no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubModuleJpaMapper — ida y vuelta dominio <-> entidad JPA")
class SubModuleJpaMapperTest {

    private final SubModuleJpaMapper mapper = new SubModuleJpaMapper();

    @Mock
    private ModuleJpaEntity moduleEntity;

    @Test
    @DisplayName("toJpa copia cada campo del dominio, incluido el modulo dado")
    void to_jpa_copia_cada_campo_del_dominio() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        SubModule subModule = new SubModule(2L, "Reportes", "REP",
                new ModuleRef(1L, "Facturacion", "FACT"), creado, true);

        SubModuleJpaEntity entity = mapper.toJpa(subModule, moduleEntity);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getName()).isEqualTo("Reportes");
        assertThat(entity.getCode()).isEqualTo("REP");
        assertThat(entity.getModule()).isSameAs(moduleEntity);
        assertThat(entity.getCreatedDate()).isEqualTo(creado);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toJpa conserva enabled=false de un submodulo deshabilitado")
    void to_jpa_conserva_enabled_false() {
        SubModule subModule = new SubModule(2L, "Reportes", "REP",
                new ModuleRef(1L, "Facturacion", "FACT"), LocalDateTime.of(2026, 1, 15, 10, 30),
                false);

        SubModuleJpaEntity entity = mapper.toJpa(subModule, moduleEntity);

        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity) reconstruye el modulo desde la relacion JPA")
    void to_domain_reconstruye_el_modulo_desde_la_relacion() {
        when(moduleEntity.getId()).thenReturn(1L);
        when(moduleEntity.getName()).thenReturn("Facturacion");
        when(moduleEntity.getCode()).thenReturn("FACT");
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        SubModuleJpaEntity entity = new SubModuleJpaEntity();
        entity.setId(2L);
        entity.setName("Reportes");
        entity.setCode("REP");
        entity.setModule(moduleEntity);
        entity.setCreatedDate(creado);
        entity.setEnabled(false);

        SubModule subModule = mapper.toDomain(entity);

        assertThat(subModule.getId()).isEqualTo(2L);
        assertThat(subModule.getName()).isEqualTo("Reportes");
        assertThat(subModule.getCode()).isEqualTo("REP");
        assertThat(subModule.getModule()).isEqualTo(new ModuleRef(1L, "Facturacion", "FACT"));
        assertThat(subModule.getCreatedDate()).isEqualTo(creado);
        assertThat(subModule.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toDomain(entity, ref) usa la referencia dada sin tocar la relacion JPA")
    void to_domain_con_ref_usa_la_referencia_dada() {
        LocalDateTime creado = LocalDateTime.of(2026, 1, 15, 10, 30);
        SubModuleJpaEntity entity = new SubModuleJpaEntity();
        entity.setId(2L);
        entity.setName("Reportes");
        entity.setCode("REP");
        entity.setCreatedDate(creado);
        entity.setEnabled(true);
        ModuleRef ref = new ModuleRef(9L, "Inventario", "INV");

        SubModule subModule = mapper.toDomain(entity, ref);

        assertThat(subModule.getModule()).isEqualTo(ref);
        assertThat(subModule.getName()).isEqualTo("Reportes");
        assertThat(subModule.getCode()).isEqualTo("REP");
    }
}
