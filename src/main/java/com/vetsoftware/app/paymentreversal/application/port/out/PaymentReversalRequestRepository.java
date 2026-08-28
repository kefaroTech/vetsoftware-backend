package com.vetsoftware.app.paymentreversal.application.port.out;

import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * <strong>No existe ningun {@code findById(Long)} ancho, y es
 * deliberado.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca
 * al caso de uso que conoce la variante ancha y no la acotada; la forma de no
 * poder equivocarse es que la ancha no exista. Toda lectura por id de este
 * slice lleva la empresa.
 */
public interface PaymentReversalRequestRepository {

    PaymentReversalRequest save(PaymentReversalRequest request);

    Optional<PaymentReversalRequest> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * El expediente que ya existe sobre ese pago. Se consulta
     * <strong>antes</strong> de insertar:
     * {@code uq_payment_reversal_requests_payment} convierte el duplicado en un
     * error, y un 500 no dice que el problema es que ya hay uno abierto.
     */
    Optional<PaymentReversalRequest> findByCompanyIdAndPaymentId(Long companyId, Long paymentId);

    PageResult<PaymentReversalRequest> findAllByCompanyId(Long companyId, int page, int pageSize);

    /**
     * Barrido de plataforma cross-tenant: expedientes <strong>sin resolver</strong>
     * cuyo plazo vence antes de la fecha dada. Solo lo consume un puerto SYSTEM.
     *
     * <p>
     * El filtro por «sin resolver» va en la consulta y no en memoria: paginar sobre
     * el conjunto sin filtrar devolveria paginas mayormente vacias segun envejece
     * la tabla, y el total del {@code PageResult} mentiria.
     */
    PageResult<PaymentReversalRequest> findAllExpiringBefore(LocalDateTime before, int page,
            int pageSize);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<PaymentReversalRequest> findAll(int page, int pageSize);
}
