package com.vetsoftware.app.shared.pricing;

import java.time.LocalDate;

/**
 * La tarifa existe y esta publicada, pero el dia contra el que se compara cae
 * fuera de su ventana de vigencia, asi que no puede poner precio a nada (D-73,
 * COT-020/COT-021).
 *
 * <p>
 * <b>Vive en el kernel compartido porque el hecho es el mismo en los dos
 * caminos.</b> Nacio en {@code quote.domain} sirviendo solo a la cotizacion;
 * cuando el camino del contrato empezo a exigir lo mismo en su cabecera, la
 * alternativa era una segunda excepcion con otro nombre y otro codigo HTTP para
 * el mismo suceso. Dos tipos para un hecho es como se acaba con dos 409
 * distintos que el front tiene que aprender por separado, asi que el tipo es
 * uno y lo comparten cotizacion y contrato.
 *
 * <p>
 * <b>Tiene tipo propio por lo mismo que
 * {@code NumberingResolutionNotEffectiveException} (#125): el hecho es
 * distinto.</b> Colapsarlo en el {@code IllegalArgumentException} de «tarifa
 * publicada no encontrada» juntaria dos cosas que se arreglan de manera
 * distinta: una lista que no existe -o que sigue en borrador- se corrige
 * eligiendo otra, y una lista caducada se corrige publicando la del periodo
 * nuevo. Con un solo mensaje para las dos, ni el front puede decir que hacer ni
 * el operador puede contar por separado el dia que el catalogo se quede sin
 * tarifa vigente, que es exactamente el dia en que deja de entrar dinero.
 *
 * <p>
 * Los campos son <b>datos para el handler</b>, no un mensaje: el detalle que ve
 * el cliente lo compone {@code GlobalExceptionHandler} a partir de ellos, de
 * modo que el texto de esta excepcion nunca llega al cliente (#118).
 */
public class PriceListNotEffectiveException extends RuntimeException {

    private final Long priceListId;
    private final String code;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final LocalDate quotedOn;

    public PriceListNotEffectiveException(Long priceListId, String code, LocalDate validFrom,
            LocalDate validTo, LocalDate quotedOn) {
        super("Price list is not effective");
        this.priceListId = priceListId;
        this.code = code;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.quotedOn = quotedOn;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    /** Codigo de negocio de la tarifa, que es como la llaman los humanos. */
    public String getCode() {
        return code;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    /** Fecha de fin de vigencia; nula si la lista no tiene cierre. */
    public LocalDate getValidTo() {
        return validTo;
    }

    /**
     * El dia contra el que se comparo, derivado del reloj del negocio (D-81).
     *
     * <p>
     * <b>Se sigue llamando {@code quotedOn} aunque ahora tambien lo emita el camino
     * del contrato.</b> El nombre viaja al cliente como propiedad del
     * {@code ProblemDetail} del 409; renombrarlo cambiaria el cuerpo de un error
     * que el front ya lee, y el ahorro seria puramente cosmetico.
     */
    public LocalDate getQuotedOn() {
        return quotedOn;
    }
}
