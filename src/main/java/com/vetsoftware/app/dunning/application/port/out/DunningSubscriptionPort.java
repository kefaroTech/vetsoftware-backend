package com.vetsoftware.app.dunning.application.port.out;

import com.vetsoftware.app.dunning.domain.DunningSubscriptionSnapshot;
import com.vetsoftware.app.dunning.domain.DunningSubscriptionStatus;
import java.util.Optional;

public interface DunningSubscriptionPort {

    Optional<DunningSubscriptionSnapshot> lockByIdAndCompanyId(Long subscriptionId, Long companyId);

    /**
     * <b>Ya no lleva motivo, y esa ausencia es el arreglo.</b> La cobranza
     * construye frases utilisimas —«Factura FE-1043 vencida hace 6 dias; gracia de
     * 5 dias agotada»— y las sigue escribiendo enteras en {@code DunningEvent}, que
     * es su bitacora. Lo que no puede es empujarlas al canal de auditoria del
     * contrato, que es vocabulario cerrado: alli el motivo se deriva del estado de
     * destino. Dejar el parametro y que el adaptador lo ignorase en silencio seria
     * peor que quitarlo; asi el compilador obliga a mirar cada sitio.
     */
    void changeStatus(Long subscriptionId, Long companyId, DunningSubscriptionStatus status,
            String actor);
}
