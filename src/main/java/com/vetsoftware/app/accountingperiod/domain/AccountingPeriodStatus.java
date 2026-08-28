package com.vetsoftware.app.accountingperiod.domain;

/**
 * En que punto de su vida esta un mes contable. Dominio cerrado y espejo
 * <strong>literal</strong> de {@code chk_accounting_periods_status}: los tres
 * nombres se escriben aqui igual que en la constraint, porque
 * {@code @Enumerated(EnumType.STRING)} guarda el {@code name()} tal cual y un
 * valor que la comprobacion no admita lo rechaza la base con un error que no
 * menciona ni la columna ni el valor.
 *
 * <p>
 * <strong>Son tres y no dos, y el estado del medio es el que hace util a toda
 * la ficha.</strong> Con solo «abierto» y «cerrado», reabrir un mes seria
 * indistinguible de no haberlo cerrado nunca. {@link #SOFT_CLOSED} es «cerrado
 * pero todavia corregible con firma»; {@link #LOCKED} es «declarado», el punto
 * a partir del cual el numero ya salio de la empresa y no puede cambiar.
 *
 * <p>
 * <strong>No hay {@code enabled} ni un cuarto estado de baja.</strong> Un
 * periodo no se desactiva: un mes que existio no deja de existir, y una fila
 * que desaparece deja al informe anual sin uno de sus doce sumandos.
 */
public enum AccountingPeriodStatus {

    /**
     * Se puede registrar. Es el estado inicial y —salvo un periodo reabierto— el
     * unico que la base admite con {@code closed_at} nulo.
     */
    OPEN,

    /**
     * Cerrado, pero reabrible con motivo escrito. Es el unico estado desde el que
     * {@code AccountingPeriod.reopen} acepta volver a {@link #OPEN}.
     */
    SOFT_CLOSED,

    /**
     * Declarado. No se toca: ni se registra en el, ni se reabre. Es el unico estado
     * terminal del ciclo.
     */
    LOCKED
}
