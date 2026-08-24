package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;

/**
 * El unico punto de este slice que sabe como esta hecha la entidad JPA de
 * {@code subscriptionbilling}.
 *
 * <p>
 * Existe como una sola clase y no repartido por el mapper y el query port: la
 * excepcion de vertical slicing permite importar la {@code XxxJpaEntity} de
 * otra feature desde {@code infrastructure/persistence}, y concentrarlo aqui
 * hace que un cambio en la forma de esa entidad se arregle en un fichero en vez
 * de en tres.
 *
 * <p>
 * {@code document_kind} se copia como texto con {@code String.valueOf} para no
 * atar este slice al tipo del enum de la otra feature: lo que necesita saber es
 * si el documento es una nota credito, no como lo modela su dueno.
 */
final class SubscriptionBillingDocumentRefs {

    private SubscriptionBillingDocumentRefs() {
    }

    static BillingDocumentRef toRef(SubscriptionBillingDocumentJpaEntity entity) {
        if (entity == null)
            return null;
        return new BillingDocumentRef(entity.getId(), entity.getCompanyId(),
                entity.getDocumentNumber(), String.valueOf(entity.getDocumentKind()),
                entity.getTotalAmount(), entity.getBalanceAmount());
    }
}
