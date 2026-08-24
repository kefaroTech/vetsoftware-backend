package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
@DisplayName("JpaCatalogItemRepository — catálogo global contra MySQL real")
class CatalogItemPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaCatalogItemRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("guarda, relee y pagina por sortOrder con desempate estable")
    void guarda_relee_y_pagina_con_orden_estable() {
        CatalogItem nuevo = CatalogItem.create("TEST_REPORTS", "Reportes", null, null,
                ItemType.MODULE, null, false, 1, 1, 50, CatalogItemStatus.ACTIVE,
                CatalogItemMother.RELOJ);

        CatalogItem guardado = repository.save(nuevo);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).get().satisfies(leido -> {
            assertThat(leido.getCode()).isEqualTo("TEST_REPORTS");
            assertThat(leido.getName()).isEqualTo("Reportes");
            assertThat(leido.getItemType()).isEqualTo(ItemType.MODULE);
        });
        assertThat(repository.findAll(0, 200).content()).extracting(CatalogItem::getId)
                .contains(guardado.getId());
        assertThat(repository.existsByCodeIgnoringEnabled("TEST_REPORTS")).isTrue();
    }

    /**
     * El {@code code} es único en base sin mirar {@code enabled}, así que un
     * artículo dado de baja sigue ocupando su código. Por eso el alta comprueba con
     * {@code existsByCodeIgnoringEnabled} y no con un {@code findByCode} corriente:
     * lo segundo diría «libre» y el {@code INSERT} moriría contra la clave única.
     */
    @Test
    @DisplayName("un articulo dado de baja sigue ocupando su codigo, invisible a las consultas normales")
    void un_articulo_de_baja_sigue_ocupando_su_codigo() {
        CatalogItem guardado = repository.save(
                CatalogItem.create("TEST_CODIGO_OCUPADO", "Ocupado", null, null, ItemType.MODULE,
                        null, false, 1, 1, 51, CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));
        entityManager.flush();

        repository.delete(guardado.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).isEmpty();
        assertThat(repository.findAll(0, 200).content()).extracting(CatalogItem::getId)
                .doesNotContain(guardado.getId());
        assertThat(repository.existsByCodeIgnoringEnabled("TEST_CODIGO_OCUPADO")).isTrue();
    }

    /**
     * {@code reactivate} mueve la {@code version} en su {@code SET}: es un
     * {@code UPDATE} masivo que no pasa por el ciclo leer-modificar-guardar de
     * Hibernate, así que sin ese incremento un {@code save} concurrente que venga
     * con la versión vieja pisaría la reactivación sin ruido.
     */
    @Test
    @DisplayName("reactivar devuelve el articulo a los listados y mueve su version")
    void reactivar_devuelve_el_articulo_y_mueve_su_version() {
        CatalogItem guardado = repository.save(
                CatalogItem.create("TEST_REACTIVABLE", "Reactivable", null, null, ItemType.MODULE,
                        null, false, 1, 1, 52, CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ));
        entityManager.flush();
        entityManager.clear();
        Long versionInicial = repository.findById(guardado.getId()).orElseThrow().getVersion();

        repository.delete(guardado.getId());
        entityManager.flush();
        entityManager.clear();
        assertThat(repository.findById(guardado.getId())).isEmpty();

        assertThat(repository.reactivate(guardado.getId())).isEqualTo(1);
        entityManager.clear();

        assertThat(repository.findById(guardado.getId())).get().satisfies(revivido -> {
            assertThat(revivido.getCode()).isEqualTo("TEST_REACTIVABLE");
            assertThat(revivido.getVersion()).isGreaterThan(versionInicial);
        });
    }

    @Test
    @DisplayName("reactivar un articulo que no existe devuelve cero filas en vez de fingir que lo revivio")
    void reactivar_un_articulo_inexistente_devuelve_cero() {
        assertThat(repository.reactivate(999_999L)).isZero();
    }

    @Test
    @DisplayName("un codigo que nadie uso nunca no figura como ocupado")
    void un_codigo_libre_no_figura_como_ocupado() {
        assertThat(repository.existsByCodeIgnoringEnabled("TEST_CODIGO_QUE_NO_EXISTE")).isFalse();
    }
}
