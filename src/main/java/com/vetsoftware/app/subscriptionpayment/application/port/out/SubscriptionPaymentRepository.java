package com.vetsoftware.app.subscriptionpayment.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <strong>No existe ningun {@code findById(Long)} ancho, y es
 * deliberado.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca
 * al caso de uso que conoce la variante ancha y no la acotada; la forma de no
 * poder equivocarse es que la ancha no exista. Toda lectura por id de este
 * slice lleva la empresa.
 */
public interface SubscriptionPaymentRepository {

    SubscriptionPayment save(SubscriptionPayment payment);

    Optional<SubscriptionPayment> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Toma un bloqueo pesimista sobre la fila del pago dentro de la transaccion en
     * curso, acotado por empresa.
     *
     * <p>
     * Es lo que serializa el <em>read-then-write</em> de R3: sin el, dos
     * aplicaciones concurrentes leen la misma suma de aplicaciones y las dos pasan
     * la comprobacion, dejando el pago sobreaplicado. Acotado por empresa por el
     * mismo motivo que el resto: la variante ancha concederia un {@code FOR UPDATE}
     * sobre la fila de otro tenant antes de cualquier comprobacion.
     */
    Optional<SubscriptionPayment> lockByIdAndCompanyId(Long id, Long companyId);

    /**
     * Pago ya registrado con esta llave de idempotencia. Se consulta
     * <strong>antes</strong> de insertar (R13): la constraint unica convierte el
     * duplicado en un error, y un 500 en la cara del cliente no es una respuesta
     * idempotente.
     */
    Optional<SubscriptionPayment> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId);

    /**
     * Pago ya registrado por el mismo aviso de la pasarela. Es la barandilla del
     * webhook: el mismo aviso recibido dos veces no crea dos pagos.
     */
    Optional<SubscriptionPayment> findByGatewayAndGatewayReference(String gateway,
            String gatewayReference);

    PageResult<SubscriptionPayment> findAllByCompanyId(Long companyId, int page, int pageSize);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<SubscriptionPayment> findAll(int page, int pageSize);

    /**
     * Pagos {@code PENDING} de pasarela envejecidos, ascendente por
     * {@code receivedAt} y acotado a {@code limit}.
     */
    List<SubscriptionPayment> findStalePendingGatewayPayments(LocalDateTime threshold, int limit);

    /**
     * Barrido de tesorería con filtros opcionales. {@code pendingThreshold} no nulo
     * añade {@code gateway IS NOT NULL AND receivedAt < pendingThreshold} —el mismo
     * criterio que {@link #findStalePendingGatewayPayments}— y ordena ascendente
     * por {@code receivedAt}; si es nulo, ordena descendente. Las dos ramas
     * desempatan por {@code id}.
     */
    PageResult<SubscriptionPayment> findAllFiltered(Long companyId,
            SubscriptionPaymentStatus status, LocalDateTime receivedFrom, LocalDateTime receivedTo,
            LocalDateTime pendingThreshold, int page, int pageSize);

    /**
     * Mismo filtro que {@link #findAllFiltered}, sin paginar: para el cierre de
     * mes.
     */
    List<SubscriptionPayment> findAllFilteredForExport(Long companyId,
            SubscriptionPaymentStatus status, LocalDateTime receivedFrom, LocalDateTime receivedTo,
            LocalDateTime pendingThreshold);
}
