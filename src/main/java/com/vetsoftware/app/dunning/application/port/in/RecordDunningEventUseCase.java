package com.vetsoftware.app.dunning.application.port.in;

import com.vetsoftware.app.dunning.application.command.RecordDunningEventCommand;
import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RecordDunningEventUseCase {

    /**
     * Anota un hito del expediente. Solo registra: la aritmetica de la mora -cuando
     * empieza la gracia, cuando toca bajar a solo lectura- no esta especificada en
     * el modelo y este slice no la inventa.
     *
     * <p>
     * <strong>SYSTEM a secas, sin camino de tenant, y aqui pesa mas que en los
     * demas.</strong> El expediente existe para demostrar que se aviso antes de
     * restringir la cuenta; si el deudor pudiera escribir en el, no demostraria
     * nada. Por eso {@code dunningEvent.create} no se siembra y solo
     * {@code dunningEvent.read} llega al ADMIN del tenant
     * ({@code 257_seed_subscriptionpayment_dunning_permissions.xml}). El
     * razonamiento general esta en {@code RegisterSubscriptionPaymentUseCase}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    DunningEventDto execute(RecordDunningEventCommand command);
}
