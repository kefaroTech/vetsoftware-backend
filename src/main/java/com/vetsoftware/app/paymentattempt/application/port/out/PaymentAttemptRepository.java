package com.vetsoftware.app.paymentattempt.application.port.out;

import com.vetsoftware.app.paymentattempt.domain.PaymentAttempt;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * <strong>No existe ningun {@code findById(Long)} ancho, y es
 * deliberado.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca
 * al caso de uso que conoce la variante ancha y no la acotada; la forma de no
 * poder equivocarse es que la ancha no exista. Toda lectura por id de este
 * slice lleva la empresa.
 *
 * <p>
 * La unica consulta sin empresa es {@link #findAllDueForRetry}, que es el
 * barrido de plataforma y solo lo consume un puerto {@code SYSTEM}.
 */
public interface PaymentAttemptRepository {

    PaymentAttempt save(PaymentAttempt attempt);

    Optional<PaymentAttempt> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<PaymentAttempt> findAllByCompanyId(Long companyId, int page, int pageSize);

    PageResult<PaymentAttempt> findAllByCompanyIdAndBillingDocumentId(Long companyId,
            Long billingDocumentId, int page, int pageSize);

    /**
     * Cola de reintentos: lo vencido de <strong>todas</strong> las empresas.
     * Barrido de plataforma; ver {@code ListDuePaymentAttemptsUseCase}.
     */
    PageResult<PaymentAttempt> findAllDueForRetry(LocalDateTime dueBefore, int page, int pageSize);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<PaymentAttempt> findAll(int page, int pageSize);

    /**
     * Ultimo numero de intento gastado sobre el documento. Vacio si es el primero.
     * Se consulta <strong>dentro de la transaccion</strong> que inserta, porque
     * {@code uq_payment_attempts_number} exige que el consecutivo no colisione.
     */
    Optional<Integer> findMaxAttemptNumber(Long companyId, Long billingDocumentId);

    /**
     * Intentos <strong>imputables al cliente</strong> sobre el documento desde
     * {@code since}. Excluye los {@code CONFIGURATION}: un fallo propio no gasta el
     * presupuesto de nadie.
     */
    int countRetryableSince(Long companyId, Long billingDocumentId, LocalDateTime since);
}
