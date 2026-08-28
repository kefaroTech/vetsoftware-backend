package com.vetsoftware.app.accountingaccount.domain;

/**
 * La clase de la cuenta, que es lo que decide el signo del asiento.
 *
 * <p>
 * Dominio cerrado y <strong>espejo exacto</strong> de
 * {@code chk_accounting_accounts_class} (changeset 342). No se deduce del
 * codigo sin conocer la norma: con la adopcion de NIIF el PUC dejo de ser
 * obligatorio y cada empresa define su propio plan, asi que el primer digito ya
 * no garantiza nada. Guardarla es lo que permite armar un balance de prueba sin
 * una tabla de traduccion escondida en el codigo.
 */
public enum AccountClass {

    /** Activo. Saldo deudor. */
    ASSET,

    /** Pasivo. Saldo acreedor. */
    LIABILITY,

    /** Patrimonio. Saldo acreedor. */
    EQUITY,

    /** Ingreso. Saldo acreedor. */
    REVENUE,

    /** Gasto. Saldo deudor. */
    EXPENSE,

    /** Costo. Saldo deudor. */
    COST
}
