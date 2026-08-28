package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapper es el único punto que conoce dominio y entidad JPA a la vez, así
 * que un campo cruzado aquí no lo detecta ninguna otra capa. Este artículo
 * tiene tres pares de campos del mismo tipo —{@code code}/{@code name},
 * {@code minQuantity}/{@code maxQuantity}/{@code sortOrder} y las dos
 * descripciones— y cruzarlos compila perfectamente.
 */
@DisplayName("CatalogItemJpaMapper — ida y vuelta dominio <-> entidad JPA")
class CatalogItemJpaMapperTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 8, 22, 10, 15, 30);

    private final CatalogItemJpaMapper mapper = new CatalogItemJpaMapper();

    private static CatalogItem usuarioExtra() {
        return new CatalogItem(2L, "EXTRA_USER", "Usuario adicional", "Un usuario más",
                "Detalle largo", ItemType.CAPACITY, "USER", false, 1, 50, 7,
                CatalogItemStatus.ACTIVE, CREADO, 4L, true);
    }

    @Test
    @DisplayName("toJpa copia cada campo del dominio sin cruzar ninguno")
    void to_jpa_copia_cada_campo() {
        CatalogItemJpaEntity entity = mapper.toJpa(usuarioExtra());

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getCode()).isEqualTo("EXTRA_USER");
        assertThat(entity.getName()).isEqualTo("Usuario adicional");
        assertThat(entity.getShortDescription()).isEqualTo("Un usuario más");
        assertThat(entity.getLongDescription()).isEqualTo("Detalle largo");
        assertThat(entity.getItemType()).isEqualTo(ItemType.CAPACITY);
        assertThat(entity.getCapacityUnit()).isEqualTo("USER");
        assertThat(entity.isCore()).isFalse();
        assertThat(entity.getMinQuantity()).isEqualTo(1);
        assertThat(entity.getMaxQuantity()).isEqualTo(50);
        assertThat(entity.getSortOrder()).isEqualTo(7);
        assertThat(entity.getStatus()).isEqualTo(CatalogItemStatus.ACTIVE);
        assertThat(entity.getCreatedDate()).isEqualTo(CREADO);
        assertThat(entity.getVersion()).isEqualTo(4L);
        assertThat(entity.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("toDomain reconstruye el artículo con las mismas invariantes")
    void to_domain_reconstruye_el_articulo() {
        CatalogItem original = usuarioExtra();

        CatalogItem vuelta = mapper.toDomain(mapper.toJpa(original));

        assertThat(vuelta.getId()).isEqualTo(original.getId());
        assertThat(vuelta.getCode()).isEqualTo(original.getCode());
        assertThat(vuelta.getName()).isEqualTo(original.getName());
        assertThat(vuelta.getShortDescription()).isEqualTo(original.getShortDescription());
        assertThat(vuelta.getLongDescription()).isEqualTo(original.getLongDescription());
        assertThat(vuelta.getItemType()).isEqualTo(original.getItemType());
        assertThat(vuelta.getCapacityUnit()).isEqualTo(original.getCapacityUnit());
        assertThat(vuelta.isCore()).isEqualTo(original.isCore());
        assertThat(vuelta.getMinQuantity()).isEqualTo(original.getMinQuantity());
        assertThat(vuelta.getMaxQuantity()).isEqualTo(original.getMaxQuantity());
        assertThat(vuelta.getSortOrder()).isEqualTo(original.getSortOrder());
        assertThat(vuelta.getStatus()).isEqualTo(original.getStatus());
        assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
        assertThat(vuelta.getVersion()).isEqualTo(original.getVersion());
        assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
    }

    @Test
    @DisplayName("los campos opcionales viajan nulos y no se rellenan con valores inventados")
    void los_campos_opcionales_viajan_nulos() {
        CatalogItem sinOpcionales = new CatalogItem(1L, "CORE", "Núcleo", null, null,
                ItemType.MODULE, null, true, 1, null, 0, CatalogItemStatus.DRAFT, CREADO, 0L, true);

        CatalogItemJpaEntity entity = mapper.toJpa(sinOpcionales);

        assertThat(entity.getShortDescription()).isNull();
        assertThat(entity.getLongDescription()).isNull();
        assertThat(entity.getCapacityUnit()).isNull();
        assertThat(entity.getMaxQuantity()).isNull();
        assertThat(mapper.toDomain(entity).getMaxQuantity()).isNull();
    }
}
