package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.catalogitem.domain.RelationType;
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
@DisplayName("JpaCatalogItemDependencyRepository — grafo comercial contra MySQL real")
class CatalogItemDependencyPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaCatalogItemRepository catalogItems;
    @Autowired
    private JpaCatalogItemDependencyRepository repository;
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
    @DisplayName("la arista REQUIRES se guarda y aparece en la consulta del grafo")
    void la_arista_requires_se_guarda_y_aparece_en_el_grafo() {
        CatalogItem dependiente = catalogItems.save(
                CatalogItem.create("TEST_DEPENDENT", "Dependiente", null, null, ItemType.MODULE,
                        null, false, 1, 1, 90, CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));
        CatalogItemDependency guardada = repository
                .save(CatalogItemDependency.create(dependiente.getId(), nucleo,
                        RelationType.REQUIRES, "Necesario", CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCatalogItemId(dependiente.getId())).singleElement()
                .satisfies(dependencia -> {
                    assertThat(dependencia.getId()).isEqualTo(guardada.getId());
                    assertThat(dependencia.getRelationType()).isEqualTo(RelationType.REQUIRES);
                });
        assertThat(repository.findAllRequiresEdges()).anySatisfy(edge -> {
            assertThat(edge.catalogItemId()).isEqualTo(dependiente.getId());
            assertThat(edge.relatedItemId()).isEqualTo(nucleo);
        });
    }

    /**
     * Igual que las otras dos tablas puente: la clave única es
     * {@code (catalog_item_id, related_item_id, relation_type)} y el borrado es
     * lógico, así que una arista retirada sigue ocupando la clave. El alta la
     * reactiva y le vuelve a aplicar la nota que traiga el comando.
     */
    @Test
    @DisplayName("una arista retirada sigue ocupando la clave y se reactiva en vez de insertar")
    void una_arista_retirada_se_reactiva_en_vez_de_insertar() {
        CatalogItem dependiente = catalogItems.save(CatalogItem.create("TEST_DEP_REVIVE",
                "Dependiente revivible", null, null, ItemType.MODULE, null, false, 1, 1, 91,
                CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));
        CatalogItemDependency guardada = repository
                .save(CatalogItemDependency.create(dependiente.getId(), nucleo,
                        RelationType.REQUIRES, "Necesario", CatalogItemMother.RELOJ));
        entityManager.flush();

        repository.delete(guardada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardada.getId())).isEmpty();
        assertThat(repository.findAnyByTriple(dependiente.getId(), nucleo, RelationType.REQUIRES))
                .get().satisfies(estado -> {
                    assertThat(estado.id()).isEqualTo(guardada.getId());
                    assertThat(estado.enabled()).isFalse();
                });

        assertThat(repository.reactivate(guardada.getId())).isEqualTo(1);
        entityManager.clear();

        assertThat(repository.findById(guardada.getId())).isPresent();
    }

    /**
     * El detector de ciclos se alimenta de {@code findAllRequiresEdges}. Si una
     * arista retirada siguiera contando, el grafo rechazaría como ciclo una
     * dependencia perfectamente legítima y nadie entendería por qué.
     */
    @Test
    @DisplayName("una arista retirada desaparece del grafo que alimenta al detector de ciclos")
    void una_arista_retirada_desaparece_del_grafo() {
        CatalogItem dependiente = catalogItems.save(CatalogItem.create("TEST_DEP_GRAFO",
                "Dependiente grafo", null, null, ItemType.MODULE, null, false, 1, 1, 92,
                CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));
        CatalogItemDependency guardada = repository
                .save(CatalogItemDependency.create(dependiente.getId(), nucleo,
                        RelationType.REQUIRES, "Necesario", CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();
        assertThat(repository.findAllRequiresEdges()).anySatisfy(
                edge -> assertThat(edge.catalogItemId()).isEqualTo(dependiente.getId()));

        repository.delete(guardada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllRequiresEdges()).noneSatisfy(
                edge -> assertThat(edge.catalogItemId()).isEqualTo(dependiente.getId()));
        assertThat(repository.existsActiveInvolving(dependiente.getId())).isFalse();
    }

    /**
     * El tipo de relación es parte de la clave única: la misma pareja con
     * {@code REQUIRES} y con {@code EXCLUDES} son dos filas distintas, y confundir
     * las dos haría que el alta reactivara la relación equivocada.
     */
    @Test
    @DisplayName("la consulta por terna distingue el tipo de relacion, que tambien es clave")
    void la_consulta_por_terna_distingue_el_tipo_de_relacion() {
        CatalogItem dependiente = catalogItems.save(
                CatalogItem.create("TEST_DEP_TIPO", "Dependiente tipo", null, null, ItemType.MODULE,
                        null, false, 1, 1, 93, CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));
        repository.save(CatalogItemDependency.create(dependiente.getId(), nucleo,
                RelationType.REQUIRES, "Necesario", CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAnyByTriple(dependiente.getId(), nucleo, RelationType.REQUIRES))
                .isPresent();
        assertThat(repository.findAnyByTriple(dependiente.getId(), nucleo, RelationType.EXCLUDES))
                .isEmpty();
    }

    @Test
    @DisplayName("reactivar una arista que no existe devuelve cero filas")
    void reactivar_una_arista_inexistente_devuelve_cero() {
        assertThat(repository.reactivate(999_999L)).isZero();
    }
}
