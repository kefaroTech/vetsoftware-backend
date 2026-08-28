package com.vetsoftware.app.accountingperiod.domain;

/**
 * Se intento cerrar el ultimo periodo abierto.
 *
 * <p>
 * <strong>Tiene que existir siempre al menos un periodo abierto, y esa es una
 * invariante del sistema entero y no de esta fila.</strong> Por eso la
 * comprueba el caso de uso contando los demas periodos {@code OPEN} y no el
 * constructor de la entidad: un agregado solo puede hablar de si mismo, y aqui
 * la pregunta es cuantos hermanos le quedan.
 *
 * <p>
 * <strong>Que se rompe si no esta.</strong> Sin ningun periodo abierto,
 * {@code ResolvePostingPeriodUseCase} no tiene donde imputar un hecho tardio y
 * <em>toda</em> escritura con efecto contable —una conciliacion que se
 * resuelve, una liquidacion de pasarela que llega— queda rechazada hasta que
 * alguien se acuerde de abrir el mes siguiente. Cerrar diciembre sin haber
 * abierto enero es la forma normal de provocarlo, y el sintoma no aparece en el
 * cierre sino al dia siguiente y en otra feature.
 *
 * <p>
 * Mapea a 409.
 */
public class LastOpenAccountingPeriodException extends RuntimeException {

    public LastOpenAccountingPeriodException(Long id) {
        super("Accounting period " + id + " is the last open one and cannot be closed");
    }
}
