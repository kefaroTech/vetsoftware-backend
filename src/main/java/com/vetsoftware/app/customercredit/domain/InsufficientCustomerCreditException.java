package com.vetsoftware.app.customercredit.domain;

import java.math.BigDecimal;

/**
 * Se intento gastar saldo que no existe.
 *
 * <p>
 * <strong>Quien la lanza no es una comprobacion previa, es el motor.</strong>
 * El {@code UPDATE} que mueve la fila resumen lleva la condicion
 * {@code balance_amount + :delta >= 0} dentro de su propio {@code WHERE}; si
 * afecta cero filas, no hay saldo y la operacion se aborta. Ese es el punto:
 * leer el saldo, decidir en memoria y escribir despues <em>no</em> equivale,
 * por mucho que el resultado se parezca en una prueba de un solo hilo.
 *
 * <p>
 * <strong>El caso concreto que evita, y que no da error y descuadra dinero del
 * cliente:</strong> saldo vivo de cien mil. La contadora lo aplica a la factura
 * de marzo desde una pestana y el proceso de renovacion lo aplica a la de abril
 * en el mismo segundo. Las dos leen cien mil, las dos escriben ochenta mil, las
 * dos confirman. Saldo menos sesenta mil, y nadie ve un error.
 *
 * <p>
 * Es un conflicto (409), no una peticion mal formada: el importe era razonable
 * cuando el cliente lo escribio.
 */
public class InsufficientCustomerCreditException extends RuntimeException {

    private final BigDecimal requested;

    public InsufficientCustomerCreditException(Long companyId, BigDecimal requested) {
        super("Insufficient customer credit for company " + companyId + " to apply " + requested);
        this.requested = requested;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}
