package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import java.time.LocalDate;
import java.util.Optional;

/**
 * La cuenta de cobro y su desglose fiscal, que se guardan y se leen juntos: las
 * líneas de impuesto son parte del mismo agregado y no tienen vida propia.
 *
 * <p>
 * <b>No hay {@code delete}.</b> Un documento no se borra: se anula
 * ({@code issue_status = 'VOIDED'}) mientras no exista fuera, y una vez existe
 * se corrige con una nota crédito encadenada.
 */
public interface BillingDocumentRepository {

    /** Guarda la cabecera y, con ella, su desglose fiscal. */
    SubscriptionBillingDocument save(SubscriptionBillingDocument document);

    Optional<SubscriptionBillingDocument> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Toma el bloqueo pesimista sobre la fila del documento antes de leer su
     * estado.
     *
     * <p>
     * Es lo que serializa el <i>read-then-write</i> de todo lo que mueve
     * {@code settled_amount}: sin él, dos aplicaciones concurrentes leen la misma
     * suma y las dos pasan. Va acotado por empresa, así que un documento de otro
     * tenant no devuelve fila y no bloquea nada.
     */
    Optional<SubscriptionBillingDocument> lockByIdAndCompanyId(Long id, Long companyId);

    PageResult<SubscriptionBillingDocument> findAllByCompanyId(Long companyId, int page,
            int pageSize);

    /**
     * <b>La lista de trabajo pendiente de cada mes</b>: lo que está calculado aquí
     * y todavía no se ha emitido fuera. Cada fila es dinero devengado que nadie
     * facturó.
     *
     * <p>
     * <b>Barrido de plataforma, sin tenant delante, y está declarado</b>: por eso
     * el único caso de uso que lo sirve está cerrado a {@code hasRole("SYSTEM")} a
     * secas ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, BE-29).
     */
    PageResult<SubscriptionBillingDocument> findAllAwaitingExternal(int page, int pageSize);

    /**
     * El barrido de mora de plataforma.
     *
     * <p>
     * El «vencida» lo pone esta consulta y no el marcador: las expresiones de
     * columna generada tienen que ser deterministas y {@code CURRENT_DATE} no lo
     * es. {@code overdue_marker} codifica «registrada y no saldada»; el
     * {@code today} que entra por parámetro pone el resto — y entra por parámetro,
     * y no se lee del reloj del adaptador, para que el caso del cambio de día se
     * pueda fijar en un test.
     *
     * <p>
     * Mismo régimen que el listado de arriba: sin empresa, solo SYSTEM.
     */
    PageResult<SubscriptionBillingDocument> findAllOverdue(LocalDate today, int page, int pageSize);

    /**
     * <b>La barandilla contra la doble facturación, por periodo exacto.</b>
     *
     * <p>
     * Réplica exacta de {@code recurring_cycle_marker} +
     * {@code uq_sbd_recurring_cycle}: factura, de ciclo, no anulada, del mismo
     * contrato y con <b>los dos extremos del periodo idénticos</b>. Agrupar por mes
     * hacía que la factura anual emitida a mitad de agosto chocara con la mensual
     * del día 1 y el cambio a plan anual fuera irregistrable; por periodo exacto
     * sigue impidiendo regenerar dos veces la factura del mismo periodo, que es la
     * doble facturación real.
     *
     * <p>
     * Es la <b>primera</b> línea de defensa —da un 409 legible en vez de una
     * violación de constraint convertida en 500—; la última sigue siendo el índice
     * único.
     */
    boolean existsRecurringCycle(Long companyId, Long subscriptionId, LocalDate periodStart,
            LocalDate periodEnd);
}
