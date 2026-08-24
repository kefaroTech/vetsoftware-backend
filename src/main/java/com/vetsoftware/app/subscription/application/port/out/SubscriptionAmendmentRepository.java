package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import java.util.Optional;

/**
 * Puerto de salida de los otrosies. Solo inserta y lee: un documento inmutable
 * no se actualiza ni se desactiva.
 */
public interface SubscriptionAmendmentRepository {

    SubscriptionAmendment save(SubscriptionAmendment amendment);

    Optional<SubscriptionAmendment> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * La llave antiduplicados. <strong>Se busca antes de insertar, dentro de la
     * transaccion</strong> — no se deja que reviente el indice unico y se atrapa la
     * excepcion: un 500 en la cara del cliente no es una respuesta idempotente, y
     * dos clics en «Anadir» no pueden generar dos cobros. Si ya existe, la peticion
     * devuelve lo que se creo la primera vez.
     *
     * <p>
     * Va acotada por empresa aunque
     * {@code uq_subscription_amendments_client_request} sea global: una clinica no
     * tiene por que poder comprobar si una llave ajena ya se uso.
     */
    Optional<SubscriptionAmendment> findByClientRequestIdAndCompanyId(String clientRequestId,
            Long companyId);

    PageResult<SubscriptionAmendment> findAllBySubscriptionIdAndCompanyId(Long subscriptionId,
            Long companyId, int page, int pageSize);
}
