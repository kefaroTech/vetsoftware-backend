package com.vetsoftware.app.catalogitemaihint.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.catalogitem.infrastructure.persistence.CatalogItemJpaRepository;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaAiHintCatalogItemQueryPort} contra MySQL real.
 *
 * <p>
 * <b>Existe porque el defecto que cubre no se puede ver desde ningun otro
 * sitio.</b> El puerto promete que {@code findById} no devuelve un articulo
 * retirado —«publicarle una pista a un articulo retirado le ensenaria al modelo
 * un codigo que el motor rechazara despues»—, y el estado comercial vive en una
 * columna, {@code catalog_items.status}, de una tabla de otra feature. Un test
 * con el puerto mockeado devuelve lo que el propio test le diga que devuelva:
 * no puede distinguir un adaptador que filtra de uno que no.
 *
 * <p>
 * &#9888; <b>Y la segunda mitad, que se midio en vez de suponerse.</b> El
 * adaptador anterior no escribia ningun filtro: delegaba en
 * {@code CatalogItemJpaRepository} y confiaba en el
 * {@code @SQLRestriction("enabled = true")} de {@code CatalogItemJpaEntity}.
 * Que esa restriccion alcance o no la carga por clave primaria es una pregunta
 * discutible —el {@code @Where} historico no lo hacia— y de su respuesta
 * dependia si los dos metodos del puerto filtraban igual. La respuesta con este
 * Hibernate es <b>si, los dos</b>, y la deja escrita con el SQL delante
 * {@link SqlRestriction#el_sql_restriction_filtra_los_dos_caminos_pero_no_el_status()}.
 * El agujero real era otro: <b>ningun camino miraba {@code status}</b>.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAiHintCatalogItemQueryPort — que articulos admite una pista, contra MySQL real")
class AiHintCatalogItemQueryPortIT extends AbstractDataJpaTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CatalogItemJpaRepository catalogItemJpaRepository;

    private JpaAiHintCatalogItemQueryPort port;
    private Long activo;
    private Long enBorrador;
    private Long retirado;
    private Long deshabilitado;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        port = new JpaAiHintCatalogItemQueryPort(entityManager);

        activo = SchemaSeed.catalogItemId(entityManager, "CORE");
        enBorrador = estado("SURGERY", "DRAFT", true);
        retirado = estado("HOSPITALIZATION", "DEPRECATED", true);
        deshabilitado = estado("LAB_IMAGING", "ACTIVE", false);
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Deja el articulo en un estado concreto con SQL directo. El
     * {@code @SQLRestriction} de {@code CatalogItemJpaEntity} impide llegar a una
     * fila deshabilitada por JPA, asi que el andamio tiene que ir por debajo del
     * mapeo.
     */
    private Long estado(String code, String status, boolean habilitado) {
        Long id = SchemaSeed.catalogItemId(entityManager, code);
        entityManager.createNativeQuery("""
                UPDATE catalog_items
                   SET status = :status, enabled = :habilitado
                 WHERE id = :id
                """).setParameter("status", status).setParameter("habilitado", habilitado)
                .setParameter("id", id).executeUpdate();
        return id;
    }

    @Nested
    @DisplayName("La guarda de publicacion — findById")
    class Guarda {

        @Test
        @DisplayName("un articulo a la venta si admite pista, con su codigo y su nombre")
        void un_articulo_activo_si_admite_pista() {
            assertThat(port.findById(activo)).get()
                    .extracting(CatalogItemRef::id, CatalogItemRef::code)
                    .containsExactly(activo, "CORE");
        }

        /**
         * &#9940; <b>El caso que da sentido a esta rodaja.</b> Un articulo
         * {@code DEPRECATED} se retiro de la venta pero sigue en la tabla, habilitado y
         * referenciado por contratos historicos. Publicarle una pista deja la pista
         * <em>vigente</em>: el prompt se arma con ella, el modelo aprende que puede
         * proponer ese modulo, y el unico que se entera de que la plataforma ya no lo
         * vende es el prospecto cuando su cotizacion falla.
         *
         * <p>
         * El adaptador anterior lo daba por bueno —heredaba del slice vecino un filtro
         * que solo mira {@code enabled}—, asi que este caso se pone rojo si alguien
         * vuelve a quitar el {@code AND status = 'ACTIVE'}.
         */
        @Test
        @DisplayName("un articulo retirado de la venta NO admite pista: no se le ensena al modelo")
        void un_articulo_retirado_no_admite_pista() {
            assertThat(port.findById(retirado))
                    .as("DEPRECATED sigue habilitado en la tabla, pero ya no se vende").isEmpty();
        }

        /**
         * El gemelo del anterior por el otro extremo del ciclo: un {@code DRAFT}
         * todavia se esta redactando. Ensenarselo al modelo es prometer algo que
         * todavia no existe.
         */
        @Test
        @DisplayName("un articulo en borrador tampoco admite pista")
        void un_articulo_en_borrador_no_admite_pista() {
            assertThat(port.findById(enBorrador)).isEmpty();
        }

        /**
         * El borrado logico. {@code ACTIVE} pero {@code enabled = false}: la fila esta
         * apagada y no cuenta, exactamente igual que si no existiera. Es el unico de
         * los tres que el adaptador anterior <b>si</b> cubria —lo ponia el
         * {@code @SQLRestriction} del slice vecino, tambien por id— y por eso sigue
         * pasando con el filtro ahora escrito a mano. Ver {@link SqlRestriction}.
         */
        @Test
        @DisplayName("un articulo deshabilitado tampoco admite pista")
        void un_articulo_deshabilitado_no_admite_pista() {
            assertThat(port.findById(deshabilitado)).isEmpty();
        }

        @Test
        @DisplayName("un id que no existe, y el id nulo, salen vacios sin tocar la base")
        void un_id_inexistente_sale_vacio() {
            assertThat(port.findById(-1L)).isEmpty();
            assertThat(port.findById(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("El pintado del listado — findAllByIds")
    class Pintado {

        /**
         * &#9940; <b>La asimetria con {@link Guarda}, y es deliberada.</b> Aqui no se
         * autoriza nada: se le pone nombre a filas del historial que ya existen. La
         * pista de un articulo {@code DEPRECATED} es justo la que mas interesa leer
         * —«por que el modelo proponia esto»— y esconder su nombre dejaria el historial
         * ilegible sin impedir ninguna escritura. Si alguien "unificara" los dos
         * metodos copiando el {@code AND status = 'ACTIVE'} aqui, este caso se pone
         * rojo y explica por que.
         */
        @Test
        @DisplayName("un articulo retirado SI sale con su nombre: el historial tiene que leerse")
        void el_retirado_si_se_pinta() {
            Map<Long, CatalogItemRef> resueltos = port
                    .findAllByIds(List.of(activo, retirado, enBorrador));

            assertThat(resueltos).containsKeys(activo, retirado, enBorrador);
            assertThat(resueltos.get(retirado).name()).isNotBlank();
        }

        /**
         * El borrado logico si corta tambien aqui: la fila esta apagada. El DTO la
         * sirve con {@code catalogItemCode} y {@code catalogItemName} nulos, que es lo
         * que su Javadoc promete.
         */
        @Test
        @DisplayName("un articulo deshabilitado no aparece en el mapa")
        void el_deshabilitado_no_se_pinta() {
            assertThat(port.findAllByIds(List.of(activo, deshabilitado))).containsKey(activo)
                    .doesNotContainKey(deshabilitado);
        }

        @Test
        @DisplayName("una peticion vacia, nula o de solo nulos no consulta nada")
        void una_peticion_vacia_no_consulta() {
            assertThat(port.findAllByIds(List.of())).isEmpty();
            assertThat(port.findAllByIds(null)).isEmpty();
            assertThat(port.findAllByIds(java.util.Collections.singletonList(null))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Que cubria de verdad el @SQLRestriction del slice vecino")
    class SqlRestriction {

        /**
         * &#9940; <b>El hecho medido, y NO es el que se suponia.</b> Este caso se
         * escribio esperando lo contrario de lo que afirma hoy: que
         * {@code @SQLRestriction} filtrara la consulta por lote pero no la carga por
         * clave primaria —el comportamiento historico de {@code @Where}, y la razon por
         * la que «se aplica o no por id» es una pregunta que se discute—. El motor lo
         * desmintio en la primera ejecucion, con el SQL delante:
         *
         * <pre>
         * findById     → … from catalog_items cije1_0 where cije1_0.id=? and (cije1_0.enabled = true)
         * findAllById  → … from catalog_items cije1_0 where (cije1_0.enabled = true) and cije1_0.id in (?)
         * </pre>
         *
         * <p>
         * Con el Hibernate que trae Spring Boot 4.1, la restriccion entra en los
         * <b>dos</b> caminos. O sea que la afirmacion del adaptador anterior —«el
         * filtro de articulo vivo lo pone el mapeo»— <b>era cierta</b>, tambien por id,
         * y no habia ninguna asimetria accidental en {@code enabled}.
         *
         * <p>
         * <b>Lo que si estaba roto, y sigue siendolo sin el arreglo.</b>
         * {@code @SQLRestriction} <em>nunca</em> ha mirado {@code status}: cubria el
         * borrado logico y dejaba pasar {@code DRAFT} y {@code DEPRECATED} por los dos
         * caminos por igual. Por eso el {@code AND status = 'ACTIVE'} del adaptador no
         * es redundante con nada — ver {@link Guarda} — y por eso el filtro se escribe
         * a mano en vez de heredarse: heredarlo ata esta guarda a una decision de mapeo
         * de otra feature que puede cambiar sin que nadie lo note aqui.
         *
         * <p>
         * <b>Este caso vale como centinela de version.</b> Si un Hibernate futuro
         * dejara de aplicar la restriccion por id, se pone rojo — y lo correcto
         * entonces sera <b>actualizar esta documentacion</b>, no tocar el adaptador: su
         * SQL ya no depende de este comportamiento.
         *
         * <p>
         * El {@code clear()} no es decorativo: sin el, la fila estaria en el cache de
         * primer nivel y {@code findById} la devolveria sin preguntarle a la base, lo
         * que confundiria el efecto del cache con el del mapeo.
         */
        @Test
        @DisplayName("@SQLRestriction filtra los DOS caminos por enabled, pero ninguno por status")
        void el_sql_restriction_filtra_los_dos_caminos_pero_no_el_status() {
            entityManager.clear();

            Optional<?> deshabilitadoPorId = catalogItemJpaRepository.findById(deshabilitado);
            List<?> deshabilitadoPorLote = catalogItemJpaRepository
                    .findAllById(List.of(deshabilitado));

            assertThat(deshabilitadoPorId)
                    .as("la carga por clave primaria SI pasa por @SQLRestriction").isEmpty();
            assertThat(deshabilitadoPorLote).as("y la consulta por lote tambien").isEmpty();

            Optional<?> retiradoPorId = catalogItemJpaRepository.findById(retirado);
            List<?> retiradoPorLote = catalogItemJpaRepository.findAllById(List.of(retirado));

            assertThat(retiradoPorId).as("pero un DEPRECATED se cuela por id: el mapeo no mira"
                    + " status, y por eso el adaptador escribe ese filtro").isPresent();
            assertThat(retiradoPorLote).as("y tambien por lote").hasSize(1);
        }
    }
}
