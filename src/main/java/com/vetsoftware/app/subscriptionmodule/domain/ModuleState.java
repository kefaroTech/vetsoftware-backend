package com.vetsoftware.app.subscriptionmodule.domain;

/**
 * El estado comercial de un módulo del catálogo, visto desde una empresa
 * concreta. No es {@code subscription_items.charge_mode}: es su traducción a lo
 * que el escaparate necesita distinguir, incluida la ausencia total de línea
 * ({@link #NOT_INCLUDED}) y el caso {@code NEVER_FREE} sin comprar.
 */
public enum ModuleState {
    TRIAL, FREE_LIMITED, EXPIRED_READ_ONLY, PAID, NOT_INCLUDED, NEVER_FREE
}
