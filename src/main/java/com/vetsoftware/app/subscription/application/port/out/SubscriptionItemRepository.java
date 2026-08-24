package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemOverlapDto;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida de las lineas del contrato. Igual que
 * {@link SubscriptionRepository}, sin variante ancha por id.
 *
 * <p>
 * No expone <strong>ningun</strong> metodo de borrado, y tampoco uno que
 * desactive: R12 dice que dar de baja un modulo jamas destruye informacion del
 * cliente. La unica baja posible es escribir {@code effective_to} en el dominio
 * y guardar.
 */
public interface SubscriptionItemRepository {

    SubscriptionItem save(SubscriptionItem item);

    List<SubscriptionItem> saveAll(List<SubscriptionItem> items);

    Optional<SubscriptionItem> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Las lineas del mismo articulo que se pisarian con el tramo
     * {@code [from, to)}, dentro del mismo contrato y de la misma empresa.
     *
     * <p>
     * Es la comprobacion que sostiene R7, y hay que llamarla <strong>tras tomar el
     * bloqueo sobre {@code subscriptions}</strong>. Cubre lo que el esquema no
     * puede: el indice unico sobre {@code current_item_marker} impide dos lineas
     * <em>abiertas</em> del mismo articulo —el caso comun— pero no dos tramos con
     * fechas de fin futuras que se solapen, porque los dos dan marcador nulo y
     * MySQL no tiene restricciones de exclusion.
     *
     * @param excludeItemId
     *            la linea que se esta editando, para que no se compare consigo
     *            misma. Puede ser {@code null} al abrir una nueva
     */
    List<SubscriptionItem> findOverlapping(Long companyId, Long subscriptionId, Long catalogItemId,
            LocalDate from, LocalDate to, Long excludeItemId);

    /**
     * La linea abierta de ese articulo en ese contrato, si la hay. Como maximo una:
     * lo garantiza {@code uq_subscription_items_current}.
     */
    Optional<SubscriptionItem> findOpenByCatalogItemId(Long companyId, Long subscriptionId,
            Long catalogItemId);

    /**
     * La linea que abrio ese otrosi. La usa el camino idempotente: si el
     * {@code clientRequestId} ya se habia procesado, hay que devolver el recurso
     * que se creo la primera vez, no crear otro.
     */
    Optional<SubscriptionItem> findByCreatedAmendmentIdAndCompanyId(Long amendmentId,
            Long companyId);

    /**
     * El expediente completo de un contrato: tambien las lineas ya cerradas, que
     * siguen ahi.
     */
    PageResult<SubscriptionItem> findAllBySubscriptionIdAndCompanyId(Long subscriptionId,
            Long companyId, int page, int pageSize);

    /**
     * Lo que estaba contratado ese dia. Aplica el criterio de vigencia
     * —{@code effective_from <= dia AND (effective_to IS NULL OR effective_to > dia)}—
     * y usa {@code ix_subscription_items_vigencia}.
     */
    PageResult<SubscriptionItem> findCurrentOn(Long subscriptionId, Long companyId, LocalDate day,
            int page, int pageSize);

    /**
     * Lo mismo, <strong>sin paginar</strong>, para el unico consumidor que necesita
     * el contrato entero y no una pagina: el prorrateo de la cancelacion, que suma
     * la cuota recurrente de todas las lineas vigentes para saber cuanto deja de
     * facturarse. Sumar sobre una pagina daria un abono corto —silenciosamente— en
     * cuanto un contrato pasara de {@code Pages.MAX_SIZE} lineas.
     */
    List<SubscriptionItem> findAllCurrentOn(Long subscriptionId, Long companyId, LocalDate day);

    /**
     * La consulta de vigilancia R7 completa, para el trabajo programado. Barre
     * todas las clinicas, asi que solo la puede consumir un caso de uso cerrado a
     * {@code hasRole('SYSTEM')} a secas. <strong>Cero filas = sano.</strong>
     */
    List<SubscriptionItemOverlapDto> findAllOverlaps();
}
