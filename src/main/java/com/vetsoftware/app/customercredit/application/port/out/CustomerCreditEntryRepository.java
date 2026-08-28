package com.vetsoftware.app.customercredit.application.port.out;

import com.vetsoftware.app.customercredit.domain.CreditLot;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * El libro de asientos.
 *
 * <p>
 * <strong>Sin ningun {@code findById(Long)} ancho, y es deliberado.</strong>
 * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca al caso de uso que
 * conoce la variante ancha y no la acotada; la forma de no poder equivocarse es
 * que la ancha no exista. Toda lectura por id de este slice lleva la empresa.
 *
 * <p>
 * <strong>Y sin ninguna escritura sobre fila existente.</strong> No hay
 * {@code update} ni {@code delete} porque el libro es estrictamente de solo
 * anadir: corregir un asiento es escribir otro que lo compensa. Esa ausencia es
 * lo que sostiene la exencion {@code E1_APPEND_ONLY} de la entidad JPA.
 */
public interface CustomerCreditEntryRepository {

    /**
     * Separa la llave de idempotencia de la operacion del sufijo por lote.
     *
     * <p>
     * Un consumo escribe una fila por lote y {@code uq_cce_idempotency} es
     * {@code (company_id, client_request_id)}: sin sufijo, las N filas de una misma
     * operacion colisionarian entre si. La frontera prohibe este caracter en la
     * llave que manda el cliente, de modo que el prefijo de una operacion no pueda
     * confundirse con el de otra.
     */
    String OPERATION_SEPARATOR = "#";

    CustomerCreditEntry save(CustomerCreditEntry entry);

    Optional<CustomerCreditEntry> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Asiento ya escrito con esta llave de idempotencia. Se consulta
     * <strong>antes</strong> de insertar: {@code uq_cce_idempotency} convierte el
     * duplicado en un error de integridad, y un 500 en la cara del cliente no es
     * una respuesta idempotente.
     */
    Optional<CustomerCreditEntry> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId);

    /**
     * Todas las filas escritas por una misma operacion de consumo, en el orden en
     * que se escribieron.
     *
     * <p>
     * Existe porque un consumo <strong>no es una sola fila</strong>: se reparte
     * entre los lotes y cada uno deja su asiento. Buscar solo por la llave del
     * cliente devolveria una de N y el reintento contestaria a medias. Se resuelve
     * por prefijo {@code clientRequestId + OPERATION_SEPARATOR}.
     */
    List<CustomerCreditEntry> findOperation(Long companyId, String clientRequestId);

    /**
     * Los lotes con saldo vivo de una empresa, <strong>ya ordenados por el que
     * antes caduca</strong> —los sin fecha al final— y con desempate por id.
     *
     * <p>
     * El orden es parte del contrato y no una cortesia: consumir por caducidad mas
     * proxima es lo que hace que no caduque nada que se pudiera haber gastado, es
     * lo mas favorable al cliente y es lo que hacen los sistemas del mercado
     * (D-71). Si el adaptador devolviera otro orden, el consumo seria correcto en
     * importe y equivocado en cual lote gasto, y la diferencia solo se veria meses
     * despues, cuando caducara saldo que no tenia por que caducar.
     */
    List<CreditLot> findOpenLotsByCompanyId(Long companyId);

    /**
     * Los lotes de una empresa cuya fecha de caducidad ya paso y todavia tienen
     * remanente. Es lo que consume el caso de uso de caducidad.
     */
    List<CreditLot> findExpiredLotsByCompanyId(Long companyId, LocalDate asOf);

    PageResult<CustomerCreditEntry> findAllByCompanyId(Long companyId, int page, int pageSize);

    /**
     * <strong>Barrido de plataforma: sin empresa a proposito.</strong> Los lotes de
     * todas las clinicas que caducan antes de una fecha. Ponerle la empresa delante
     * lo haria inutil —es justo lo que hay que recorrer entero— y por eso el unico
     * caso de uso que lo consume esta cerrado a {@code hasRole('SYSTEM')} a secas.
     */
    PageResult<CustomerCreditEntry> findAllExpiringBefore(LocalDate before, int page, int pageSize);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<CustomerCreditEntry> findAll(int page, int pageSize);
}
