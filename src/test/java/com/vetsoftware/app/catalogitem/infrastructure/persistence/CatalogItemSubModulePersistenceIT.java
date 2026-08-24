package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.catalogitem.domain.SubModuleRef;
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
@DisplayName("JpaCatalogItemSubModuleRepository — vínculo catálogo/submódulo contra MySQL real")
class CatalogItemSubModulePersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaCatalogItemRepository catalogItems;
    @Autowired
    private JpaCatalogItemSubModuleRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("hidrata el SubModuleRef desde la FK real al releer el vínculo")
    void hidrata_el_submodule_ref_desde_la_fk_real() {
        CatalogItem item = catalogItems
                .save(CatalogItem.create("TEST_LINK", "Vínculo test", null, null, ItemType.MODULE,
                        null, false, 1, 1, 70, CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));
        SubModuleRef subModule = new SubModuleRef(SchemaSeed.SUB_MODULE_ID, "Submodulo de prueba",
                "TEST_SUB_MODULE");
        CatalogItemSubModule guardado = repository.save(
                CatalogItemSubModule.create(item.getId(), subModule, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).get().satisfies(vinculo -> {
            assertThat(vinculo.getCatalogItemId()).isEqualTo(item.getId());
            assertThat(vinculo.getSubModule().id()).isEqualTo(SchemaSeed.SUB_MODULE_ID);
            assertThat(vinculo.getSubModule().code()).isEqualTo("TEST_SUB_MODULE");
        });
    }

    /**
     * Tercera tabla puente con la misma trampa: clave única
     * {@code (catalog_item_id, sub_module_id)} y borrado lógico. Un vínculo
     * retirado sigue ocupando la clave, así que volver a vincular el mismo
     * submódulo tiene que reactivar la fila, no insertar otra.
     */
    @Test
    @DisplayName("un vinculo retirado sigue ocupando la clave y se reactiva en vez de insertar")
    void un_vinculo_retirado_se_reactiva_en_vez_de_insertar() {
        CatalogItem item = catalogItems.save(CatalogItem.create("TEST_LINK_REVIVE",
                "Vínculo revivible", null, null, ItemType.MODULE, null, false, 1, 1, 71,
                CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));
        SubModuleRef subModule = new SubModuleRef(SchemaSeed.SUB_MODULE_ID, "Submodulo de prueba",
                "TEST_SUB_MODULE");
        CatalogItemSubModule guardado = repository.save(
                CatalogItemSubModule.create(item.getId(), subModule, CatalogItemMother.RELOJ));
        entityManager.flush();

        repository.delete(guardado.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).isEmpty();
        assertThat(repository.findAllByCatalogItemId(item.getId())).isEmpty();
        assertThat(repository.existsActiveByCatalogItemId(item.getId())).isFalse();
        assertThat(repository.findAnyByPair(item.getId(), SchemaSeed.SUB_MODULE_ID)).get()
                .satisfies(estado -> {
                    assertThat(estado.id()).isEqualTo(guardado.getId());
                    assertThat(estado.enabled()).isFalse();
                });

        assertThat(repository.reactivate(guardado.getId())).isEqualTo(1);
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).get()
                .satisfies(revivido -> assertThat(revivido.getSubModule().code())
                        .isEqualTo("TEST_SUB_MODULE"));
        assertThat(repository.existsActiveByCatalogItemId(item.getId())).isTrue();
    }

    @Test
    @DisplayName("reactivar un vinculo que no existe devuelve cero filas")
    void reactivar_un_vinculo_inexistente_devuelve_cero() {
        assertThat(repository.reactivate(999_999L)).isZero();
    }
}
