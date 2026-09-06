package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.DueRetryTarget;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDateTime;

/**
 * La cola de reintentos vencidos, que es de otra feature
 * ({@code paymentattempt}), detrás de su propio puerto {@code SYSTEM}
 * ({@code ListDuePaymentAttemptsUseCase}).
 */
public interface DueRetryQueryPort {
    PageResult<DueRetryTarget> listDue(LocalDateTime now, int page, int pageSize);
}
