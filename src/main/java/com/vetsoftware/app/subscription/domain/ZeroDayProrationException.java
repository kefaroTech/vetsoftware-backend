package com.vetsoftware.app.subscription.domain;

import java.time.LocalDate;

/**
 * Un prorrateo de <b>cero dias</b>: el tramo que el otrosi afecta no toca ni un
 * dia del periodo en curso.
 *
 * <p>
 * <b>Esto es un error, nunca un resultado, y confundirlo con un resultado costo
 * dinero.</b> Antes de esta clase la formula devolvia {@code 0.00} y el otrosi
 * se guardaba con importe cero, firmado, inmutable y con toda la pinta de estar
 * bien. El caso real: el periodo en curso del contrato <b>nunca avanzaba</b>
 * —{@code Subscription.renewPeriod} no tenia llamador— asi que seguia siendo el
 * de la firma; meses despues, la conversion de una linea de prueba a pago se
 * medi­a contra un periodo ya cerrado, no lo cruzaba, y el cliente estrenaba su
 * plan de pago con un cargo de cero pesos. El sistema no emitia ni un aviso: en
 * {@code subscription_amendments} un importe de cero es indistinguible de un
 * cambio que de verdad no movia dinero.
 *
 * <p>
 * <b>Extiende {@link IllegalArgumentException}</b> a proposito: el
 * {@code GlobalExceptionHandler} ya la traduce a 400, que es la respuesta
 * correcta —quien pidio el cambio dio una fecha efectiva fuera del periodo que
 * se esta facturando— y no hace falta tocar un fichero compartido para
 * conseguirla.
 */
public class ZeroDayProrationException extends IllegalArgumentException {

    public ZeroDayProrationException(LocalDate periodStart, LocalDate periodEnd) {
        super("El cambio no afecta ni un dia del periodo en curso (" + periodStart + ".."
                + periodEnd + "): un prorrateo de cero dias es un error, no un importe de cero."
                + " Revisa la fecha efectiva del cambio y que el periodo facturado del contrato"
                + " este al dia.");
    }
}
