package com.vetsoftware.app.dunning.infrastructure.persistence;

import com.vetsoftware.app.dunning.domain.BillingDocumentRef;
import com.vetsoftware.app.dunning.domain.SubscriptionRef;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaEntity;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;

/**
 * El unico punto de este slice que sabe como estan hechas las entidades JPA de
 * {@code subscription} y {@code subscriptionbilling}.
 *
 * <p>
 * La excepcion de vertical slicing permite importarlas desde
 * {@code infrastructure/persistence}; concentrarlo en una clase hace que un
 * cambio en la forma de esas entidades se arregle en un fichero en vez de en
 * tres. Notese que las dos <strong>no mapean la empresa igual</strong> -el
 * contrato con una asociacion, el documento con un escalar-, y ese detalle solo
 * hay que recordarlo aqui.
 *
 * <p>
 * El estado del contrato se copia con {@code String.valueOf} para no atar este
 * slice al tipo del enum de la otra feature.
 */
final class DunningRefs {

    private DunningRefs() {
    }

    static SubscriptionRef toRef(SubscriptionJpaEntity entity) {
        if (entity == null)
            return null;
        return new SubscriptionRef(entity.getId(), entity.getCompany().getId(),
                entity.getSubscriptionNumber(), String.valueOf(entity.getStatus()));
    }

    static BillingDocumentRef toRef(SubscriptionBillingDocumentJpaEntity entity) {
        if (entity == null)
            return null;
        return new BillingDocumentRef(entity.getId(), entity.getCompanyId(),
                entity.getDocumentNumber(), entity.getBalanceAmount());
    }
}
