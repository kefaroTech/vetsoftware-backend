package com.vetsoftware.app.accountingperiod.domain;

/**
 * Ya hay un periodo con esa clave de mes.
 *
 * <p>
 * Es el espejo en Java de {@code uq_accounting_periods_period}. Existe para que
 * abrir dos veces el mismo mes —lo que pasa cuando el cierre mensual se ejecuta
 * a mano y ademas por el programador de tareas, o cuando dos personas abren el
 * ejercicio a la vez— conteste un conflicto legible en vez de un 500 con un
 * {@code Duplicate entry} del driver.
 *
 * <p>
 * <strong>Es un conflicto y no un cuerpo mal formado</strong>: la peticion esta
 * bien escrita y lo que choca es el estado de la tabla en este instante. Mapea
 * a 409.
 */
public class AccountingPeriodAlreadyExistsException extends RuntimeException {

    public AccountingPeriodAlreadyExistsException(AccountingPeriodKey periodKey) {
        super("Accounting period already exists: " + periodKey);
    }
}
