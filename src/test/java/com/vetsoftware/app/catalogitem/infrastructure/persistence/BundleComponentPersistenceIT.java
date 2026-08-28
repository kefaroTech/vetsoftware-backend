package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaBundleComponentRepository — componentes de paquete contra MySQL real")
class BundleComponentPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaCatalogItemRepository catalogItems;
    @Autowired
    private JpaBundleComponentRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
    }

    @Test
    @DisplayName("persiste la pareja de artículos y la consulta conserva la cantidad")
    void persiste_la_pareja_y_conserva_la_cantidad() {
        CatalogItem bundle = catalogItems
                .save(CatalogItem.create("TEST_BUNDLE", "Paquete test", null, null, ItemType.BUNDLE,
                        null, false, 1, 1, 80, CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));

        BundleComponent guardado = repository
                .save(BundleComponent.create(bundle.getId(), nucleo, 3, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByBundleItemId(bundle.getId())).singleElement()
                .satisfies(componente -> {
                    assertThat(componente.getId()).isEqualTo(guardado.getId());
                    assertThat(componente.getComponentItemId()).isEqualTo(nucleo);
                    assertThat(componente.getQuantity()).isEqualTo(3);
                });
        assertThat(repository.findAnyByPair(bundle.getId(), nucleo)).get()
                .extracting(state -> state.enabled()).isEqualTo(true);
    }

    /**
     * La pieza que solo se puede probar contra una base real: la tabla lleva
     * borrado lógico y una clave única sobre las dos FK, así que una fila dada de
     * baja <strong>sigue ocupando la clave siendo invisible</strong>. El alta tiene
     * que reactivarla; si insertara, la clave única reventaría con un error de
     * integridad que el usuario lee como «error inesperado».
     */
    @Test
    @DisplayName("una fila dada de baja sigue ocupando la clave y el alta la reactiva en vez de insertar")
    void una_fila_de_baja_se_reactiva_en_vez_de_insertar() {
        CatalogItem bundle = catalogItems.save(CatalogItem.create("TEST_BUNDLE_REVIVE",
                "Paquete revivible", null, null, ItemType.BUNDLE, null, false, 1, 1, 81,
                CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));
        BundleComponent guardado = repository
                .save(BundleComponent.create(bundle.getId(), nucleo, 3, CatalogItemMother.RELOJ));
        entityManager.flush();

        repository.delete(guardado.getId());
        entityManager.flush();
        entityManager.clear();

        // Invisible por el @SQLRestriction, pero la fila y su clave siguen ahi.
        assertThat(repository.findById(guardado.getId())).isEmpty();
        assertThat(repository.findAllByBundleItemId(bundle.getId())).isEmpty();
        assertThat(repository.existsActiveInvolving(bundle.getId())).isFalse();
        assertThat(repository.findAnyByPair(bundle.getId(), nucleo)).get().satisfies(estado -> {
            assertThat(estado.id()).isEqualTo(guardado.getId());
            assertThat(estado.enabled()).isFalse();
        });

        assertThat(repository.reactivate(guardado.getId())).isEqualTo(1);
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).get()
                .satisfies(revivido -> assertThat(revivido.getQuantity()).isEqualTo(3));
        assertThat(repository.existsActiveInvolving(nucleo)).isTrue();
    }

    @Test
    @DisplayName("reactivar una fila que no existe devuelve cero filas en vez de fingir que la revivio")
    void reactivar_una_fila_inexistente_devuelve_cero() {
        assertThat(repository.reactivate(999_999L)).isZero();
    }

    @Test
    @DisplayName("una pareja que nunca existio no devuelve estado de vinculo")
    void una_pareja_inexistente_no_devuelve_estado() {
        assertThat(repository.findAnyByPair(999_998L, 999_999L)).isEmpty();
    }
}
