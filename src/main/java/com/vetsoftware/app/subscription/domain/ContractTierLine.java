package com.vetsoftware.app.subscription.domain;

/**
 * Un tramo ya convertido en las dos cifras con las que se abre una linea de
 * contrato.
 *
 * <p>
 * <b>Lo incluido viaja SOLO en el primer tramo</b>, y esa es toda la sutileza
 * de este tipo. La linea del contrato guarda {@code quantity} e
 * {@code included_quantity} y factura la resta de las dos, asi que repetir lo
 * incluido en cada tramo lo regalaria tantas veces como tramos haya. Poniendolo
 * una vez, la suma de las lineas devuelve exactamente lo contratado y cada
 * linea factura las unidades de SU tramo.
 */
public record ContractTierLine(ContractPriceTier tier, int quantity, int includedQuantity) {
}
