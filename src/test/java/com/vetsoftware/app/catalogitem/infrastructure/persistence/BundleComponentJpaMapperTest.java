package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BundleComponentJpaMapper — ida y vuelta dominio <-> entidad JPA")
class BundleComponentJpaMapperTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 8, 22, 10, 15, 30);

    private final BundleComponentJpaMapper mapper = new BundleComponentJpaMapper();

    private static CatalogItemJpaEntity articulo(Long id) {
        CatalogItemJpaEntity entity = new CatalogItemJpaEntity();
        entity.setId(id);
        return entity;
    }

    /**
     * Cruzar paquete y pieza compila y produce un paquete que dice contenerse a sí
     * mismo al revés: el pack pasaría a ser componente de lo que debía incluir.
     */
    @Test
    @DisplayName("toJpa no cruza el paquete con la pieza")
    void to_jpa_no_cruza_el_paquete_con_la_pieza() {
        BundleComponent component = new BundleComponent(70L, 3L, 2L, 5, CREADO, true);

        BundleComponentJpaEntity entity = mapper.toJpa(component, articulo(3L), articulo(2L));

        assertThat(entity.getId()).isEqualTo(70L);
        assertThat(entity.getBundleItem().getId()).isEqualTo(3L);
        assertThat(entity.getComponentItem().getId()).isEqualTo(2L);
        assertThat(entity.getQuantity()).isEqualTo(5);
        assertThat(entity.getCreatedDate()).isEqualTo(CREADO);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toDomain reconstruye la pieza con su cantidad")
    void to_domain_reconstruye_la_pieza() {
        BundleComponent original = new BundleComponent(70L, 3L, 2L, 5, CREADO, false);

        BundleComponent vuelta = mapper
                .toDomain(mapper.toJpa(original, articulo(3L), articulo(2L)));

        assertThat(vuelta.getId()).isEqualTo(70L);
        assertThat(vuelta.getBundleItemId()).isEqualTo(3L);
        assertThat(vuelta.getComponentItemId()).isEqualTo(2L);
        assertThat(vuelta.getQuantity()).isEqualTo(5);
        assertThat(vuelta.getCreatedDate()).isEqualTo(CREADO);
        assertThat(vuelta.isEnabled()).isFalse();
    }
}
