package com.vetsoftware.app.registration.application.port.out;

/**
 * Crea el contrato inicial de una empresa recien dada de alta.
 *
 * <p>
 * <b>Regla del modelo, no negociable:</b> toda empresa nace con un contrato. El
 * alta de la empresa y la creacion de su suscripcion ocurren en la
 * <em>misma</em> transaccion; no existe una empresa sin contrato vigente. Una
 * empresa sin contrato no tiene {@code company_entitlements}, y sin ellos entra
 * al sistema y no puede hacer absolutamente nada — el peor modo de fallo
 * posible, porque parece un problema de permisos del usuario y se investiga en
 * el sitio equivocado.
 *
 * <p>
 * Por eso este puerto no devuelve nada opcional y no admite un camino «sin
 * contrato»: o crea el contrato, o lanza
 * {@link com.vetsoftware.app.registration.domain.PlatformCatalogNotConfiguredException}
 * y el alta entera revierte.
 */
public interface InitialSubscriptionCreator {

    /**
     * @param companyId
     *            id de la empresa recien creada, dentro de la misma transaccion del
     *            alta
     * @param companyName
     *            nombre de la empresa, solo para que el mensaje de error sea
     *            legible por un humano
     */
    void createInitialContract(Long companyId, String companyName);
}
