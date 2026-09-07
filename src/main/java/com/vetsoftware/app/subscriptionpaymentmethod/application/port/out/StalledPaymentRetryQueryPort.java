package com.vetsoftware.app.subscriptionpaymentmethod.application.port.out;

import java.util.List;

/**
 * Los intentos de cobro varados de otra feature ({@code paymentattempt}) que
 * hay que reactivar tras fijar una tarjeta nueva como predeterminada.
 */
public interface StalledPaymentRetryQueryPort {

    List<Long> findStalledLastAttemptIds(Long companyId);
}
