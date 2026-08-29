package com.vetsoftware.app.configurator.domain;

/**
 * Ciclo de facturacion con el que se resuelve una seleccion.
 *
 * <p>
 * <strong>Es la tercera copia del enumerado en el proyecto</strong> —ya existen
 * {@code quote.domain.BillingCycle} y {@code pricelist.domain.BillingCycle}— y
 * eso es el vertical slicing funcionando, no una duplicacion por descuido: si
 * manana un slice anade un ciclo, los otros dos siguen diciendo lo que decian.
 *
 * <p>
 * <strong>Lo que si seria un problema es que dos de ellas se asomaran al
 * contrato.</strong> Springdoc fusiona esquemas por nombre simple, asi que dos
 * {@code BillingCycle} distintos con el mismo nombre colisionarian en
 * {@code api/openapi.json}. Por eso este enumerado <em>no sale del slice</em>:
 * el campo del request es un {@code String} con {@code @Pattern} y
 * {@code @Schema(allowableValues = ...)}, exactamente como
 * {@code SelfServeQuoteRequest}, que ya dejo escrito el razonamiento.
 */
public enum BillingCycle {
    MONTHLY, ANNUAL
}
