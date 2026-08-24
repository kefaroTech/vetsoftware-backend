package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.application.dto.QuoteTotalsMismatchDto;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteSummary;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida de la cotizacion. La cabecera es la FRONTERA DE TENANT del
 * bloque: las lineas y las respuestas no llevan company_id y solo se alcanzan
 * pasando por aqui.
 */
public interface QuoteRepository {

    Quote save(Quote quote);

    /**
     * Carga ancha, para el camino SYSTEM sobre una oferta a prospecto -que no tiene
     * empresa a la que acotar-. Todo caso de uso que la use tiene que usar TAMBIEN
     * {@link #findByIdAndCompanyId} en la misma clase: es el ternario legitimo que
     * exige CARGA_POR_ID_ACOTADA_POR_EMPRESA.
     */
    Optional<Quote> findById(Long id);

    Optional<Quote> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * La llave antiduplicados (R13), SIN acotar. Se consulta ANTES de insertar,
     * dentro de la misma transaccion: la constraint unica convierte el duplicado en
     * un error, y un 500 en la cara del cliente no es una respuesta idempotente.
     *
     * <p>
     * <strong>Es el camino SYSTEM, y solo el camino SYSTEM.</strong> Una oferta a
     * prospecto tiene {@code company_id} nulo y ningun {@code WHERE company_id = ?}
     * casaria con ella, asi que la rama ancha hace falta de verdad. Todo caso de
     * uso que la use tiene que usar TAMBIEN
     * {@link #findByClientRequestIdAndCompanyId} en la misma clase: es el mismo
     * ternario legitimo que rige el par {@link #findById} /
     * {@link #findByIdAndCompanyId}.
     */
    Optional<Quote> findByClientRequestId(String clientRequestId);

    /**
     * La llave antiduplicados ACOTADA por empresa: la que sirve al tenant.
     *
     * <p>
     * Existe porque la ancha, servida a un principal de empresa, es una lectura
     * cross-tenant disfrazada de idempotencia. {@code quotes.client_request_id} lo
     * elige quien llama, asi que una empleada de la clinica A que reutilice el
     * {@code clientRequestId} de la clinica B recibiria la cotizacion de B entera
     * —razon social, datos del prospecto, cada linea con su precio unitario y su
     * descuento negociado, los totales y la prueba de aceptacion con su IP— por un
     * endpoint de escritura y sin dejar rastro de lectura. El {@code @PreAuthorize}
     * no lo impide: {@code isMyCompany(A)} solo prueba que declara SU empresa, no
     * de quien es la fila que se va a leer.
     *
     * <p>
     * El indice unico de {@code client_request_id} sigue siendo GLOBAL a proposito
     * y no se toca: {@code company_id} es nulable, y una clave compuesta con
     * columna nulable admitiria varios {@code NULL} en MySQL, rompiendo la
     * deduplicacion de prospectos justo en el caso para el que existe. Acotar aqui
     * es una barrera de lectura, no de unicidad. Es el mismo patron que
     * {@code SubscriptionAmendmentRepository.findByClientRequestIdAndCompanyId}.
     */
    Optional<Quote> findByClientRequestIdAndCompanyId(String clientRequestId, Long companyId);

    PageResult<QuoteSummary> findAllByCompanyId(Long companyId, int page, int pageSize);

    /**
     * Listado SIN filtro de empresa: la consola de plataforma. Solo lo puede servir
     * un puerto de entrada abierto a hasRole('SYSTEM') a secas
     * (LISTADOS_SIN_EMPRESA_SOLO_SYSTEM).
     */
    PageResult<QuoteSummary> findAll(int page, int pageSize);

    /**
     * Las que ya vencieron y siguen vivas. Barrido de plataforma, sin tenant, por
     * el mismo motivo por el que ix_quotes_expiring no empieza por company_id.
     */
    List<Quote> findExpirable(LocalDate today, int batchSize);

    /**
     * Vigilancia de R5: las cotizaciones vivas en SENT o ACCEPTED cuya cabecera no
     * cuadra con la suma de sus lineas ACTIVAS.
     *
     * <p>
     * Tiene que ser SQL nativo, y no por gusto: {@code quote_lines} se lee a
     * proposito SIN {@code @SQLRestriction} —ocultar las lineas desactivadas
     * volveria ilegible la cotizacion entera—, asi que el filtro {@code l.enabled =
     * TRUE} que define el descuadre hay que escribirlo a mano. Es exactamente la
     * consulta que {@code QuoteLineJpaEntity} cita en su justificacion.
     *
     * <p>
     * Sin paginar y sin acotar por empresa: es un barrido de plataforma y lo sano
     * es que devuelva cero filas. El dia que devuelva miles, el problema no es la
     * pagina. Incidencia #428.
     */
    List<QuoteTotalsMismatchDto> findAllTotalsMismatches();

    /** Baja logica acotada por empresa. */
    void softDelete(Long id, Long companyId);

    /**
     * Baja logica sin empresa: unico camino posible para una cotizacion a
     * prospecto, cuyo company_id es NULL y por tanto no casa con ningun WHERE
     * company_id = ?. Es la sobrecarga SYSTEM declarada.
     */
    void softDelete(Long id);
}
