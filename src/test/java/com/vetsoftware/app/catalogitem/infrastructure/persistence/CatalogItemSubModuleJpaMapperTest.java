package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import com.vetsoftware.app.catalogitem.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code SubModuleJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene lógica:
 * es un portador de datos, y mockearlo no oculta comportamiento. Mismo criterio
 * que {@code SubModuleJpaMapperTest} con {@code ModuleJpaEntity}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogItemSubModuleJpaMapper — los dos caminos, lectura y escritura")
class CatalogItemSubModuleJpaMapperTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 8, 22, 10, 15, 30);

    private final CatalogItemSubModuleJpaMapper mapper = new CatalogItemSubModuleJpaMapper();

    @Mock
    private SubModuleJpaEntity subModuleEntity;

    private static CatalogItemJpaEntity articulo(Long id) {
        CatalogItemJpaEntity entity = new CatalogItemJpaEntity();
        entity.setId(id);
        return entity;
    }

    @Test
    @DisplayName("toJpa copia cada campo y planta las dos asociaciones que le dan")
    void to_jpa_copia_cada_campo() {
        CatalogItemSubModule link = new CatalogItemSubModule(5L, 1L,
                new SubModuleRef(50L, "Consultas", "CONSULTATIONS"), CREADO, true);

        CatalogItemSubModuleJpaEntity entity = mapper.toJpa(link, articulo(1L), subModuleEntity);

        assertThat(entity.getId()).isEqualTo(5L);
        assertThat(entity.getCatalogItem().getId()).isEqualTo(1L);
        assertThat(entity.getSubModule()).isSameAs(subModuleEntity);
        assertThat(entity.getCreatedDate()).isEqualTo(CREADO);
        assertThat(entity.isEnabled()).isTrue();
        verifyNoInteractions(subModuleEntity);
    }

    @Test
    @DisplayName("el camino de lectura arma el companion VO desde el submódulo ya hidratado")
    void camino_de_lectura_arma_el_companion_vo() {
        when(subModuleEntity.getId()).thenReturn(50L);
        when(subModuleEntity.getName()).thenReturn("Consultas");
        when(subModuleEntity.getCode()).thenReturn("CONSULTATIONS");

        CatalogItemSubModuleJpaEntity entity = new CatalogItemSubModuleJpaEntity();
        entity.setId(5L);
        entity.setCatalogItem(articulo(1L));
        entity.setSubModule(subModuleEntity);
        entity.setCreatedDate(CREADO);
        entity.setEnabled(true);

        CatalogItemSubModule link = mapper.toDomain(entity);

        assertThat(link.getId()).isEqualTo(5L);
        assertThat(link.getCatalogItemId()).isEqualTo(1L);
        assertThat(link.getSubModule())
                .isEqualTo(new SubModuleRef(50L, "Consultas", "CONSULTATIONS"));
        assertThat(link.getCreatedDate()).isEqualTo(CREADO);
        assertThat(link.isEnabled()).isTrue();
    }

    /**
     * El camino de escritura no puede tocar el submódulo: después de un
     * {@code getReferenceById} eso inicializaría el proxy y añadiría un SELECT por
     * cada alta. Por eso la aserción es {@code verifyNoInteractions}, y no vale
     * comprobar solo el valor devuelto.
     */
    @Test
    @DisplayName("el camino de escritura reutiliza el ref y no toca el proxy del submódulo")
    void camino_de_escritura_no_toca_el_proxy() {
        CatalogItemSubModuleJpaEntity entity = new CatalogItemSubModuleJpaEntity();
        entity.setId(5L);
        entity.setCatalogItem(articulo(1L));
        entity.setSubModule(subModuleEntity);
        entity.setCreatedDate(CREADO);
        entity.setEnabled(true);

        SubModuleRef precargado = new SubModuleRef(50L, "Consultas", "CONSULTATIONS");

        CatalogItemSubModule link = mapper.toDomain(entity, precargado);

        assertThat(link.getSubModule()).isSameAs(precargado);
        verifyNoInteractions(subModuleEntity);
    }
}
