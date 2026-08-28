package com.vetsoftware.app.accountingperiod.domain;

/**
 * No hay ningun periodo abierto en el que registrar un hecho.
 *
 * <p>
 * Es el final del camino de {@code ResolvePostingPeriodUseCase}: la fecha del
 * hecho cae en un mes que no esta abierto y <strong>no existe ningun mes
 * abierto posterior</strong> al que llevarlo. La salida nunca es imputarlo
 * hacia atras —eso reescribiria un informe ya declarado—, asi que la operacion
 * se rechaza y queda a la vista lo que falta: abrir el periodo siguiente.
 *
 * <p>
 * Mapea a 409 y no a 404 aunque hable de algo que no se encuentra: lo que falta
 * no es el recurso que el cliente pidio por id, es un estado del sistema que
 * impide completar la operacion.
 */
public class NoOpenAccountingPeriodException extends RuntimeException {

    public NoOpenAccountingPeriodException(AccountingPeriodKey from) {
        super("No open accounting period on or after " + from);
    }
}
