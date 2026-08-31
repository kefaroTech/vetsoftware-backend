package com.vetsoftware.app.catalogitemaihint.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemAiHint;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaCatalogItemAiHintRepository} contra MySQL real.
 *
 * <p>
 * <b>Los casos estan escritos desde el contrato del puerto, no desde el SQL del
 * adaptador.</b> Lo que se afirma aqui es lo que
 * {@code CatalogItemAiHintRepository} promete —«{@code supersede} escribe antes
 * de devolver», «{@code findLastRevision} mira el historial entero»,
 * «{@code existsPublishedText} pregunta por la misma clave que el indice»— y no
 * la forma en que hoy lo cumple. Un caso escrito mirando la consulta reproduce
 * el defecto en vez de cazarlo.
 *
 * <p>
 * <b>La tabla llega vacia y eso no es casualidad.</b> El changeset 382
 * condiciona su siembra a que exista un {@code system_users} habilitado, y en
 * la base de Testcontainers esa tabla esta vacia cuando corre Liquibase: no
 * inserta nada, en silencio y a proposito. Por eso estos casos pueden publicar
 * la revision 1 de {@code CORE} sin chocar con nada.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCatalogItemAiHintRepository — el historial de pistas contra MySQL real")
class CatalogItemAiHintPersistenceIT extends AbstractDataJpaTest {

    private static final String TEXTO_V1 = """
            Se necesita cuando el negocio ofrece bano y peluqueria.

            Senales en el texto: "peluqueria", "bano", "estetica".

            NO se necesita si el negocio es solo clinico.""";

    private static final String TEXTO_V2 = """
            Se necesita cuando el negocio ofrece bano, peluqueria o guarderia.

            Senales en el texto: "peluqueria", "bano", "spa", "guarderia".

            NO se necesita si el animal se queda porque esta enfermo: eso es
            hospitalizacion.""";

    private static final LocalDateTime PUBLICADO_EL = LocalDateTime.of(2026, 3, 1, 12, 0, 0);
    private static final LocalDateTime SUCEDIDO_EL = LocalDateTime.of(2026, 9, 1, 12, 0, 0);

    @Autowired
    private CatalogItemAiHintJpaRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaCatalogItemAiHintRepository repository;
    private Long articulo;
    private Long otroArticulo;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        articulo = SchemaSeed.catalogItemId(entityManager, "CORE");
        otroArticulo = SchemaSeed.catalogItemId(entityManager, "SCHEDULING");
        repository = new JpaCatalogItemAiHintRepository(springDataRepository,
                new CatalogItemAiHintJpaMapper());
    }

    private CatalogItemAiHint publicar(Long catalogItemId, int revision, String texto,
            LocalDateTime cuando) {
        CatalogItemAiHint guardada = repository.save(CatalogItemAiHint.publish(catalogItemId,
                revision, texto, SchemaSeed.SYSTEM_USER_ID, cuando, cuando));
        entityManager.flush();
        entityManager.clear();
        return guardada;
    }

    private CatalogItemAiHint suceder(CatalogItemAiHint hint, LocalDateTime cuando) {
        CatalogItemAiHint cargada = repository.findById(hint.getId()).orElseThrow();
        cargada.supersede(cuando, SchemaSeed.SYSTEM_USER_ID);
        CatalogItemAiHint sucedida = repository.supersede(cargada);
        entityManager.flush();
        entityManager.clear();
        return sucedida;
    }

    @Nested
    @DisplayName("Corregir")
    class Corregir {

        /**
         * <b>El caso que da sentido a toda la feature.</b> Corregir es publicar una
         * revision, no editar un texto: si esto se pusiera verde con la anterior
         * borrada o con su {@code hint_text} reescrito, la tabla habria dejado de poder
         * responder «con que texto se genero aquella propuesta», que es la unica
         * pregunta que justifica que exista {@code hint_revision}.
         *
         * <p>
         * Afirma las tres cosas por separado y a proposito: que la fila <b>sigue
         * ahi</b> (por id), que su <b>texto no cambio</b> y que quedo <b>marcada</b>.
         * Solo la tercera pasaria si alguien sustituyera el historial por un update en
         * sitio con auditoria aparte.
         */
        @Test
        @DisplayName("corregir marca la anterior y NO la borra: su texto sigue intacto")
        void corregir_deja_la_anterior_marcada_y_no_la_borra() {
            CatalogItemAiHint primera = publicar(articulo, 1, TEXTO_V1, PUBLICADO_EL);
            suceder(primera, SUCEDIDO_EL);
            CatalogItemAiHint segunda = publicar(articulo, 2, TEXTO_V2, SUCEDIDO_EL);

            assertThat(repository.findById(primera.getId())).as("la revision 1 sigue existiendo")
                    .get().satisfies(anterior -> {
                        assertThat(anterior.getHintText()).isEqualTo(TEXTO_V1);
                        assertThat(anterior.getHintRevision()).isEqualTo(1);
                        assertThat(anterior.getSupersededAt()).isEqualTo(SUCEDIDO_EL);
                        assertThat(anterior.isCurrent()).isFalse();
                        assertThat(anterior.getPublishedBySystemUserId())
                                .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                    });
            assertThat(repository.findCurrentByCatalogItemId(articulo)).get().satisfies(vigente -> {
                assertThat(vigente.getId()).isEqualTo(segunda.getId());
                assertThat(vigente.getHintText()).isEqualTo(TEXTO_V2);
                assertThat(vigente.getHintRevision()).isEqualTo(2);
            });

            PageResult<CatalogItemAiHint> historial = repository.findAllByCatalogItemId(articulo, 0,
                    20);
            assertThat(historial.totalElements()).as("las dos revisiones estan en el historial")
                    .isEqualTo(2L);
            assertThat(historial.content()).extracting(CatalogItemAiHint::getHintRevision)
                    .as("de la mas nueva a la mas vieja").containsExactly(2, 1);
        }

        /**
         * <b>La restriccion de verdad, y disparada por el motivo correcto.</b> La tabla
         * tiene tres indices unicos y el escenario esta montado para que solo pueda
         * saltar el que se anuncia:
         *
         * <ul>
         * <li>{@code uq_catalog_item_ai_hints_revision (catalog_item_id, hint_revision)}
         * no salta porque la segunda lleva el numero 2, no el 1.</li>
         * <li>{@code uq_catalog_item_ai_hints_text (catalog_item_id, hint_hash)} no
         * salta porque el texto es otro.</li>
         * <li>Queda {@code uq_catalog_item_ai_hints_current}, y
         * {@code EngineConstraint.assertViolates} exige que sea ese el nombre que
         * aparece en la cadena de causas: si manana saltara otro, este caso se pone
         * rojo en vez de aplaudir.</li>
         * </ul>
         *
         * <p>
         * <b>Que se rompe si falta.</b> Dos pistas vigentes para el mismo articulo
         * hacen que «que se le esta diciendo al modelo sobre GROOMING» tenga dos
         * respuestas, y el prompt se arma con una de las dos al azar segun el orden de
         * lectura.
         */
        @Test
        @DisplayName("dos pistas vigentes del mismo articulo las rechaza el motor")
        void dos_vigentes_a_la_vez_las_rechaza_el_motor() {
            publicar(articulo, 1, TEXTO_V1, PUBLICADO_EL);

            EngineConstraint.assertViolates("uq_catalog_item_ai_hints_current",
                    () -> publicar(articulo, 2, TEXTO_V2, SUCEDIDO_EL));
        }

        /**
         * El bloqueo optimista de BE-26 sobre esta tabla. Cerrar la vigencia es un
         * {@code UPDATE} sobre una fila viva, asi que Hibernate le pone
         * {@code AND version = ?}: dos correcciones simultaneas del mismo articulo no
         * se pisan en silencio.
         */
        @Test
        @DisplayName("suceder sube la version de la fila: el UPDATE lleva su chequeo optimista")
        void suceder_sube_la_version() {
            CatalogItemAiHint primera = publicar(articulo, 1, TEXTO_V1, PUBLICADO_EL);
            assertThat(primera.getVersion()).isZero();

            suceder(primera, SUCEDIDO_EL);

            assertThat(repository.findById(primera.getId())).get()
                    .extracting(CatalogItemAiHint::getVersion).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Retirar")
    class Retirar {

        /**
         * <b>Retirar es media correccion, y esa mitad que falta es la que rompe la
         * numeracion si nadie mira.</b> Despues de retirar, el articulo no tiene
         * vigente pero su historial sigue teniendo la revision 1: un servicio que
         * calculara el numero siguiente contando vigentes —cero— publicaria otra
         * revision 1 y chocaria contra {@code uq_catalog_item_ai_hints_revision}. Que
         * {@code findLastRevision} devuelva 1 con cero vigentes es exactamente lo que
         * lo evita, y es lo que promete el contrato del puerto.
         */
        @Test
        @DisplayName("tras retirar no hay vigente, pero findLastRevision sigue viendo la retirada")
        void retirar_y_volver_a_publicar_continua_la_numeracion() {
            CatalogItemAiHint primera = publicar(articulo, 1, TEXTO_V1, PUBLICADO_EL);
            suceder(primera, SUCEDIDO_EL);

            assertThat(repository.findCurrentByCatalogItemId(articulo))
                    .as("el articulo pasa a no tener pista").isEmpty();
            assertThat(repository.findLastRevision(articulo))
                    .as("pero el historial recuerda hasta donde llego la numeracion").contains(1);

            CatalogItemAiHint tercera = publicar(articulo, 2, TEXTO_V2, SUCEDIDO_EL);

            assertThat(tercera.getHintRevision()).isEqualTo(2);
            assertThat(repository.findAllByCatalogItemId(articulo, 0, 20).totalElements())
                    .isEqualTo(2L);
        }

        /**
         * &#9940; <b>La firma de la retirada llega a la columna del changeset 393 y
         * vuelve.</b> El camino entero —dominio, mapeador, entidad, MySQL, y de vuelta
         * al dominio— en un solo caso, porque es el unico sitio donde se puede afirmar:
         * un mapeador al que se le olvide una de las dos direcciones deja el dato
         * perdido en silencio y ningun test con el repositorio mockeado lo nota.
         *
         * <p>
         * <b>Y la vigente no lleva firma</b>, que es la otra mitad de
         * {@code chk_catalog_item_ai_hints_superseded_by}: nadie ha retirado lo que
         * sigue rigiendo. Si el mapeador escribiera ahi cualquier cosa —el firmante de
         * publicacion, por ejemplo— el motor abortaria el INSERT y este caso lo veria.
         */
        @Test
        @DisplayName("la firma de quien retira viaja hasta superseded_by y vuelve; la vigente no")
        void la_firma_de_la_retirada_va_y_vuelve() {
            CatalogItemAiHint primera = publicar(articulo, 1, TEXTO_V1, PUBLICADO_EL);
            assertThat(primera.getSupersededBySystemUserId())
                    .as("recien publicada no la ha retirado nadie").isNull();

            suceder(primera, SUCEDIDO_EL);

            assertThat(repository.findById(primera.getId())).get().satisfies(retirada -> {
                assertThat(retirada.getSupersededAt()).isEqualTo(SUCEDIDO_EL);
                assertThat(retirada.getSupersededBySystemUserId())
                        .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
            });
            assertThat(springDataRepository.findById(primera.getId()).orElseThrow()
                    .getSupersededBySystemUserId()).as("y esta en la columna, no solo en Java")
                    .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
        }
    }

    @Nested
    @DisplayName("La huella que sostiene uq_catalog_item_ai_hints_text")
    class Huella {

        /**
         * &#9940; <b>El unico sitio donde esta igualdad se puede afirmar.</b>
         * {@code hint_hash} es una columna generada por MySQL
         * ({@code SHA2(hint_text, 256)}) y {@link CatalogItemAiHint#hashOf} la
         * reproduce en Java. Toda la guarda de {@code existsPublishedText} depende de
         * que las dos den lo mismo: si divergieran —por codificacion, por
         * normalizacion, por lo que sea— la guarda diria «no existe», la peticion
         * llegaria a la base y el indice la pararia con un 500 en vez del 409 que el
         * front sabe pintar. Ningun test con el repositorio mockeado puede ver esto.
         */
        @Test
        @DisplayName("la huella que calcula Java es la misma que genera MySQL")
        void la_huella_de_java_es_la_que_calcula_mysql() {
            CatalogItemAiHint guardada = publicar(articulo, 1, TEXTO_V1, PUBLICADO_EL);

            CatalogItemAiHintJpaEntity fila = springDataRepository.findById(guardada.getId())
                    .orElseThrow();

            assertThat(fila.getHintHash()).isEqualTo(CatalogItemAiHint.hashOf(TEXTO_V1))
                    .matches("^[0-9a-f]{64}$");
        }

        /**
         * La guarda mira el historial entero, no solo lo vigente: republicar el texto
         * que se retiro hace un mes sigue siendo llenar el historico de revisiones
         * identicas, que es lo que la restriccion existe para impedir.
         */
        @Test
        @DisplayName("existsPublishedText ve tambien las revisiones ya reemplazadas")
        void exists_published_text_ve_el_historico() {
            CatalogItemAiHint primera = publicar(articulo, 1, TEXTO_V1, PUBLICADO_EL);
            suceder(primera, SUCEDIDO_EL);

            assertThat(repository.existsPublishedText(articulo, TEXTO_V1)).isTrue();
            assertThat(repository.existsPublishedText(articulo, TEXTO_V2)).isFalse();
            assertThat(repository.existsPublishedText(otroArticulo, TEXTO_V1))
                    .as("la unicidad es por articulo, no global").isFalse();
        }

        /**
         * La restriccion de verdad. El escenario cierra antes la vigencia de la primera
         * —con {@code supersede}, no con {@code save}— para que
         * {@code uq_catalog_item_ai_hints_current} no salte primero, y usa el numero 2
         * para que tampoco lo haga {@code uq_catalog_item_ai_hints_revision}: lo unico
         * que queda para romperse es el indice del texto.
         */
        @Test
        @DisplayName("republicar el mismo texto bajo el mismo articulo lo rechaza el motor")
        void republicar_el_mismo_texto_lo_rechaza_el_motor() {
            CatalogItemAiHint primera = publicar(articulo, 1, TEXTO_V1, PUBLICADO_EL);
            suceder(primera, SUCEDIDO_EL);

            EngineConstraint.assertViolates("uq_catalog_item_ai_hints_text",
                    () -> publicar(articulo, 2, TEXTO_V1, SUCEDIDO_EL));
        }
    }

    @Nested
    @DisplayName("Listado de vigentes")
    class Vigentes {

        @Test
        @DisplayName("solo devuelve la revision que rige, no el historial")
        void solo_devuelve_las_vigentes() {
            CatalogItemAiHint primera = publicar(articulo, 1, TEXTO_V1, PUBLICADO_EL);
            suceder(primera, SUCEDIDO_EL);
            publicar(articulo, 2, TEXTO_V2, SUCEDIDO_EL);
            publicar(otroArticulo, 1, TEXTO_V1, PUBLICADO_EL);

            assertThat(repository.findAllCurrent(0, 50).content())
                    .allMatch(CatalogItemAiHint::isCurrent)
                    .extracting(CatalogItemAiHint::getCatalogItemId)
                    .contains(articulo, otroArticulo);
            assertThat(repository.findAllCurrent(0, 50).content())
                    .filteredOn(hint -> hint.getCatalogItemId().equals(articulo))
                    .as("del articulo corregido solo aparece la revision 2").singleElement()
                    .extracting(CatalogItemAiHint::getHintRevision).isEqualTo(2);
        }
    }
}
