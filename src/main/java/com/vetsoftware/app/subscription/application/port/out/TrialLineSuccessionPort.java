package com.vetsoftware.app.subscription.application.port.out;

import java.time.LocalDate;

public interface TrialLineSuccessionPort {

    /**
     * @return {@code false} si no hay línea {@code TRIAL} abierta de ese artículo
     *         —una compra a mitad de prueba ya la cerró y dejó la sucesora
     *         {@code PAID} escrita— y por tanto no se escribe nada.
     */
    boolean transitionIfOpen(Long companyId, Long catalogItemId, LocalDate trialEndDate,
            String successorChargeMode);
}
