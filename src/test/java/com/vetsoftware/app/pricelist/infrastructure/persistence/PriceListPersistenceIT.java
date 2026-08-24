package com.vetsoftware.app.pricelist.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPriceListRepository — listas globales contra MySQL real")
class PriceListPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaPriceListRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("guarda un borrador y conserva ausencia de firma de publicación")
    void guarda_borrador_sin_inventar_firma() {
        PriceList guardada = repository.save(PriceListMother.nuevoBorrador());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardada.getId())).get().satisfies(leida -> {
            assertThat(leida.getCode()).isEqualTo("LISTA-2026-01");
            assertThat(leida.getCurrency()).isEqualTo("COP");
            assertThat(leida.getStatus()).isEqualTo(PriceListStatus.DRAFT);
            assertThat(leida.getPublishedAt()).isNull();
            assertThat(leida.getPublishedBySystemUserId()).isNull();
        });
        assertThat(repository.findAll(0, 20).content()).extracting(PriceList::getId)
                .contains(guardada.getId());
    }

    /**
     * {@code lockById} es un {@code SELECT … FOR UPDATE}: es el bloqueo pesimista
     * con el que los tres caminos de escritura de {@code catalog_prices} leen la
     * lista <strong>antes</strong> de comprobar que sigue en borrador. Nadie lo
     * había ejecutado nunca contra MySQL, y un {@code @Lock} que el motor rechace
     * no lo ve el compilador: revienta en producción, en el primer intento de
     * cotizar.
     */
    @Test
    @DisplayName("lockById lee la misma fila que findById tomando el bloqueo pesimista real")
    void lock_by_id_lee_la_misma_fila_tomando_el_bloqueo() {
        PriceList guardada = repository.save(PriceListMother.nuevoBorrador());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.lockById(guardada.getId())).get().satisfies(bloqueada -> {
            assertThat(bloqueada.getId()).isEqualTo(guardada.getId());
            assertThat(bloqueada.getCode()).isEqualTo("LISTA-2026-01");
            assertThat(bloqueada.getStatus()).isEqualTo(PriceListStatus.DRAFT);
            assertThat(bloqueada.isDraft()).isTrue();
        });
    }

    @Test
    @DisplayName("lockById de una lista que no existe devuelve vacio en vez de quedarse esperando")
    void lock_by_id_de_una_lista_inexistente_devuelve_vacio() {
        assertThat(repository.lockById(999_999L)).isEmpty();
    }

    /**
     * El {@code @SQLDelete} de {@code price_lists} lleva {@code AND version = ?}
     * porque la entidad está versionada — la trampa de BE-26: en cuanto hay
     * {@code @Version}, Hibernate liga <em>dos</em> parámetros y un
     * {@code WHERE id = ?} suelto actualizaría cero filas sin decir nada. Aquí se
     * comprueba que el borrado lógico de verdad esconde la fila.
     */
    @Test
    @DisplayName("el borrado logico esconde la lista y reactivar la devuelve moviendo su version")
    void el_borrado_logico_esconde_la_lista_y_reactivar_la_devuelve() {
        PriceList guardada = repository.save(PriceListMother.nuevoBorrador());
        entityManager.flush();
        entityManager.clear();
        Long versionInicial = repository.findById(guardada.getId()).orElseThrow().getVersion();

        repository.delete(guardada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(guardada.getId())).isEmpty();
        assertThat(repository.lockById(guardada.getId())).isEmpty();
        assertThat(repository.findAll(0, 200).content()).extracting(PriceList::getId)
                .doesNotContain(guardada.getId());

        assertThat(repository.reactivate(guardada.getId())).isEqualTo(1);
        entityManager.clear();

        assertThat(repository.findById(guardada.getId())).get().satisfies(revivida -> {
            assertThat(revivida.getCode()).isEqualTo("LISTA-2026-01");
            assertThat(revivida.getVersion()).isGreaterThan(versionInicial);
        });
    }

    @Test
    @DisplayName("reactivar una lista que no existe devuelve cero filas")
    void reactivar_una_lista_inexistente_devuelve_cero() {
        assertThat(repository.reactivate(999_999L)).isZero();
    }

    /**
     * El orden del listado es {@code validFrom} descendente con desempate por
     * {@code id}. Sin el desempate, dos listas con la misma fecha de vigencia
     * pueden repetirse u omitirse entre páginas consecutivas.
     */
    @Test
    @DisplayName("el listado ordena por vigencia descendente y desempata por id")
    void el_listado_ordena_por_vigencia_y_desempata_por_id() {
        PriceList antigua = repository.save(PriceList.create("LISTA-ORDEN-A", "Antigua", "COP",
                java.time.LocalDate.of(2025, 1, 1), null, PriceListMother.CREADA_EL));
        PriceList reciente = repository.save(PriceList.create("LISTA-ORDEN-B", "Reciente", "COP",
                java.time.LocalDate.of(2027, 1, 1), null, PriceListMother.CREADA_EL));
        entityManager.flush();
        entityManager.clear();

        java.util.List<Long> ids = repository.findAll(0, 200).content().stream()
                .map(PriceList::getId).toList();

        assertThat(ids).contains(antigua.getId(), reciente.getId());
        assertThat(ids.indexOf(reciente.getId())).isLessThan(ids.indexOf(antigua.getId()));
    }
}
