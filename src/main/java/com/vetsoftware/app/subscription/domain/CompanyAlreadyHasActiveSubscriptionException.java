package com.vetsoftware.app.subscription.domain;

/**
 * Una empresa, un contrato vivo. Lo garantiza
 * {@code uq_subscriptions_active_company} sobre la columna generada
 * {@code active_marker}, y esta excepcion es como se traduce esa violacion de
 * unique al conflicto de negocio que de verdad es.
 *
 * <p>
 * <strong>El codigo no puede comprobarlo antes y darlo por bueno.</strong> Un
 * {@code SELECT} previo y un {@code INSERT} despues es una carrera: dos altas
 * simultaneas leen las dos «no hay contrato» y las dos insertan. El indice
 * unico es la unica autoridad, asi que el camino correcto es intentar la
 * escritura y traducir el rechazo — no adivinarlo.
 *
 * <p>
 * GlobalExceptionHandler: <strong>409</strong>,
 * {@code COMPANY_ALREADY_HAS_ACTIVE_SUBSCRIPTION}.
 */
public class CompanyAlreadyHasActiveSubscriptionException extends RuntimeException {
    public CompanyAlreadyHasActiveSubscriptionException(Long companyId) {
        super("Company already has a current subscription: " + companyId);
    }
}
