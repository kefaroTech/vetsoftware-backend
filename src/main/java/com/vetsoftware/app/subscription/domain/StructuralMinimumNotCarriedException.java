package com.vetsoftware.app.subscription.domain;

import java.util.Set;

/**
 * El contrato nuevo dejaria a la empresa sin una capacidad sin la cual no puede
 * operar, y ni la cotizacion aceptada ni el contrato al que sustituye la
 * conceden.
 *
 * <p>
 * <strong>El defecto que esta excepcion existe para impedir.</strong> Sustituir
 * el contrato inicial por el que el cliente acaba de comprar es, mirado desde
 * las capacidades, <em>borrar unas lineas y escribir otras</em>. Si la
 * cotizacion no incluye {@code BRANCH} y {@code USER} —y una cotizacion de
 * modulos no tiene por que incluirlos—, el contrato nuevo se firmaria sin ellos
 * y {@code company_capacities} quedaria a cero: la empresa no podria crear ni
 * la sede que ya tiene. Habriamos convertido una compra en una degradacion, y
 * en el peor momento posible, que es justo despues de que el cliente pagara.
 *
 * <p>
 * <strong>Se falla cerrado y no se inventa un techo.</strong> Mismo criterio
 * que {@code CreateInitialSubscriptionService.requireOperableMinimum}: un techo
 * que no sale de ninguna linea de contrato es el unico numero del modelo sin
 * origen auditable. El modelo ya tiene nombre y mecanismo para lo que se
 * concede sin contrato ({@code EntitlementSource.MANUAL_GRANT}).
 *
 * <p>
 * GlobalExceptionHandler: <strong>409</strong>,
 * {@code STRUCTURAL_MINIMUM_NOT_CARRIED}.
 */
public class StructuralMinimumNotCarriedException extends RuntimeException {

    public StructuralMinimumNotCarriedException(Long companyId, Set<String> missingUnits) {
        super("The new contract of company " + companyId
                + " would not grant the structural minimum, and the contract it replaces"
                + " does not grant it either: " + String.join(", ", missingUnits));
    }
}
