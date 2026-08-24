package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CatalogItemDependencyJpaMapper — ida y vuelta dominio <-> entidad JPA")
class CatalogItemDependencyJpaMapperTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 8, 22, 10, 15, 30);

    private final CatalogItemDependencyJpaMapper mapper = new CatalogItemDependencyJpaMapper();

    private static CatalogItemJpaEntity articulo(Long id) {
        CatalogItemJpaEntity entity = new CatalogItemJpaEntity();
        entity.setId(id);
        return entity;
    }

    /**
     * Los dos extremos son del mismo tipo y cruzarlos compila: una regla «A
     * requiere B» guardada como «B requiere A» invierte el configurador entero y no
     * lo detecta ninguna otra capa.
     */
    @Test
    @DisplayName("toJpa no cruza el artículo sujeto con el relacionado")
    void to_jpa_no_cruza_los_dos_extremos() {
        CatalogItemDependency dependency = new CatalogItemDependency(9L, 1L, 2L,
                RelationType.REQUIRES, "Necesitas caja", CREADO, true);

        CatalogItemDependencyJpaEntity entity = mapper.toJpa(dependency, articulo(1L),
                articulo(2L));

        assertThat(entity.getId()).isEqualTo(9L);
        assertThat(entity.getCatalogItem().getId()).isEqualTo(1L);
        assertThat(entity.getRelatedItem().getId()).isEqualTo(2L);
        assertThat(entity.getRelationType()).isEqualTo(RelationType.REQUIRES);
        assertThat(entity.getNote()).isEqualTo("Necesitas caja");
        assertThat(entity.getCreatedDate()).isEqualTo(CREADO);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toDomain reconstruye la regla conservando el sentido")
    void to_domain_conserva_el_sentido() {
        CatalogItemDependency original = new CatalogItemDependency(9L, 1L, 2L,
                RelationType.EXCLUDES, null, CREADO, false);

        CatalogItemDependency vuelta = mapper
                .toDomain(mapper.toJpa(original, articulo(1L), articulo(2L)));

        assertThat(vuelta.getId()).isEqualTo(9L);
        assertThat(vuelta.getCatalogItemId()).isEqualTo(1L);
        assertThat(vuelta.getRelatedItemId()).isEqualTo(2L);
        assertThat(vuelta.getRelationType()).isEqualTo(RelationType.EXCLUDES);
        assertThat(vuelta.getNote()).isNull();
        assertThat(vuelta.getCreatedDate()).isEqualTo(CREADO);
        assertThat(vuelta.isEnabled()).isFalse();
    }
}
