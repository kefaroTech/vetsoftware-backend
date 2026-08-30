package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.quote.domain.QuoteStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data de la cotizacion.
 *
 * <p>
 * Hay <b>dos familias de lectura</b> a proposito. Las de detalle traen la
 * cabecera con sus lineas y sus respuestas; las de listado traen SOLO la
 * cabecera con su empresa. No es una optimizacion menor: una consulta paginada
 * que ademas hace fetch de una coleccion obliga a Hibernate a paginar en
 * memoria -trae todas las filas y recorta despues-, y ahi la paginacion deja de
 * servir para nada. Como los cuatro totales estan guardados en la cabecera, el
 * listado no necesita las lineas para decir cuanto suma cada oferta.
 *
 * <p>
 * Las dos colecciones se declaran {@code Set} y no {@code List}: dos
 * {@code List} sin columna de orden son dos <i>bags</i>, y Hibernate rechaza
 * traerlas juntas con {@code MultipleBagFetchException}. Con {@code Set} el
 * producto cartesiano de la union se deduplica solo, y el orden de impresion lo
 * pone el mapper por {@code line_number}, que es un dato y no un accidente de
 * recuperacion.
 */
public interface QuoteJpaRepository extends JpaRepository<QuoteJpaEntity, Long> {

    /**
     * Vigilancia de R5, literal del documento de reglas.
     *
     * <p>
     * Nativa y no JPQL por el {@code l.enabled = TRUE} del {@code LEFT JOIN}:
     * {@code QuoteLineJpaEntity} no lleva {@code @SQLRestriction} a proposito, asi
     * que ese filtro no lo pone nadie por nosotros. Y va en la condicion del JOIN y
     * no en el {@code WHERE}: en el {@code WHERE} convertiria el {@code LEFT JOIN}
     * en interno y las cotizaciones que se quedaron sin ninguna linea activa —el
     * descuadre mas grave— desaparecerian del informe.
     *
     * <p>
     * Solo LEE. {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} mira
     * {@code UPDATE}/{@code DELETE} y aqui no hay ninguno; que no acote por empresa
     * es el punto de un barrido de plataforma.
     */
    @Query(value = """
            SELECT q.id, q.quote_number, q.company_id,
                   q.discount_amount, COALESCE(SUM(l.discount_amount), 0),
                   q.tax_amount,      COALESCE(SUM(l.tax_amount), 0),
                   q.total_amount,    COALESCE(SUM(l.line_total), 0)
              FROM quotes q
              LEFT JOIN quote_lines l ON l.quote_id = q.id AND l.enabled = TRUE
             WHERE q.enabled = TRUE
               AND q.status IN ('SENT', 'ACCEPTED')
             GROUP BY q.id, q.quote_number, q.company_id,
                      q.discount_amount, q.tax_amount, q.total_amount
            HAVING q.discount_amount <> COALESCE(SUM(l.discount_amount), 0)
                OR q.tax_amount      <> COALESCE(SUM(l.tax_amount), 0)
                OR q.total_amount    <> COALESCE(SUM(l.line_total), 0)
             ORDER BY q.id
            """, nativeQuery = true)
    List<Object[]> findAllTotalsMismatches();

    @Override
    @EntityGraph(attributePaths = {"company", "lines"})
    Optional<QuoteJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"company", "lines"})
    Optional<QuoteJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    /**
     * La busqueda de idempotencia (R13). Trae el detalle porque el reintento tiene
     * que recibir exactamente la misma respuesta que el primer intento, lineas
     * incluidas.
     */
    @EntityGraph(attributePaths = {"company", "lines"})
    Optional<QuoteJpaEntity> findByClientRequestId(String clientRequestId);

    /**
     * La misma busqueda de idempotencia, ACOTADA por empresa: la del tenant. Trae
     * el mismo detalle porque el reintento tiene que recibir exactamente la misma
     * respuesta que el primer intento.
     *
     * <p>
     * El {@code AND company_id = ?} es lo que impide que reutilizar la llave de
     * otra clinica devuelva la cotizacion de esa otra clinica. El indice unico de
     * {@code client_request_id} es global y se queda como esta.
     */
    @EntityGraph(attributePaths = {"company", "lines"})
    Optional<QuoteJpaEntity> findByClientRequestIdAndCompany_Id(String clientRequestId,
            Long companyId);

    @EntityGraph(attributePaths = {"company", "lines"})
    List<QuoteJpaEntity> findAllByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "company")
    Page<QuoteJpaEntity> findAllByCompany_Id(Long companyId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "company")
    Page<QuoteJpaEntity> findAll(Pageable pageable);

    /**
     * Ids de las cotizaciones vencidas que siguen vivas, para el barrido de
     * plataforma.
     *
     * <p>
     * Devuelve ids y no entidades a proposito: la carga del detalle va despues, en
     * {@link #findAllByIdIn}, y asi la pagina se acota en la base en vez de en
     * memoria. Es el patron de dos pasos que {@code Pages.result(Page, List)}
     * existe para servir.
     */
    @Query("""
            SELECT q.id
            FROM QuoteJpaEntity q
            WHERE q.status IN :statuses
              AND q.validUntil < :today
            ORDER BY q.validUntil ASC, q.id ASC
            """)
    Page<Long> findExpirableIds(@Param("statuses") Collection<QuoteStatus> statuses,
            @Param("today") LocalDate today, Pageable pageable);

    // Baja logica por UPDATE nativo, NUNCA por deleteById(). El @SQLDelete de la
    // entidad solo sustituye el DELETE de la raiz: el cascade a quote_lines lo
    // emite Hibernate antes y sin interceptar, asi que deleteById() dejaria la
    // cabecera pausada y las copias congeladas -que son la prueba de lo que se le
    // ofrecio al cliente- borradas de la base.
    //
    // El UPDATE mueve tambien `version` (#53). Sin eso, un save cargado antes de la
    // baja reescribe `enabled` con su valor viejo -el mapper lo copia desde el
    // dominio- y su WHERE version = ? casa igual, con lo que una edicion
    // concurrente resucita en silencio la cotizacion que la baja acababa de pausar.
    // Movida la version, ese save no encuentra fila y salta
    // ObjectOptimisticLockingFailureException -> 409 CONCURRENT_MODIFICATION.
    // `version` NO va en el WHERE: dar de baja es deliberado y debe ejecutarse
    // siempre, no competir con una edicion.
    //
    // El AND company_id es la barrera de tenant.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE quotes
            SET enabled = false, version = version + 1
            WHERE id = :id
              AND company_id = :companyId
            """, nativeQuery = true)
    int softDelete(@Param("id") Long id, @Param("companyId") Long companyId);

    // Sobrecarga ANCHA declarada, y el unico camino posible para una cotizacion a
    // prospecto: su company_id es NULL y ningun `WHERE company_id = ?` casaria
    // jamas con ella. Es el camino SYSTEM que MUTACIONES_SQL_ACOTADAS_POR_EMPRESA
    // exime explicitamente cuando existe la sobrecarga acotada del mismo nombre
    // -la de arriba-; el gate real es el @PreAuthorize de DeleteQuoteUseCase, que
    // solo deja llegar aqui con companyId nulo a un principal SYSTEM.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE quotes
            SET enabled = false, version = version + 1
            WHERE id = :id
            """, nativeQuery = true)
    int softDelete(@Param("id") Long id);
}
