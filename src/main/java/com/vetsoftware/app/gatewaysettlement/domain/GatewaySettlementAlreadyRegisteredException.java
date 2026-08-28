package com.vetsoftware.app.gatewaysettlement.domain;

/**
 * Ya hay un lote de esa pasarela con esa referencia.
 *
 * <p>
 * Es el espejo en Java de {@code uq_gateway_settlements_reference}. Existe para
 * que recargar dos veces el mismo informe de liquidacion —el error mas comun de
 * este proceso, porque el fichero se descarga a mano de la consola de la
 * pasarela— conteste un conflicto legible en vez de un 500 con un
 * {@code Duplicate entry} del driver.
 *
 * <p>
 * <strong>La unicidad es del PAR</strong>, no de la referencia sola: dos
 * pasarelas distintas pueden numerar sus lotes igual, y una unicidad solo por
 * referencia rechazaria el segundo proveedor el dia que se contrate.
 *
 * <p>
 * <strong>Y la comparacion es exacta</strong>, igual que la columna
 * ({@code ascii_bin}): {@code LOTE-9F2A} y {@code lote-9f2a} son lotes
 * distintos y los dos entran.
 */
public class GatewaySettlementAlreadyRegisteredException extends RuntimeException {

    public GatewaySettlementAlreadyRegisteredException(String gateway, String settlementReference) {
        super("Gateway settlement already registered for " + gateway + " reference "
                + settlementReference);
    }
}
