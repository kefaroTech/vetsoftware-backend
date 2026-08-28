package com.vetsoftware.app.gatewaysettlement.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta de una liquidacion de la pasarela.
 *
 * <p>
 * <strong>Sin {@code companyId}, y aqui no hay ninguno que pudiera ir.</strong>
 * La tabla no tiene empresa porque el lote agrupa los cobros de muchas
 * clinicas: no es la omision defensiva de un recurso scoped, es que el dato no
 * existe — un lote no es de nadie en particular.
 *
 * <p>
 * <strong>Los cinco importes viajan sueltos y no como
 * {@code SettlementAmounts}.</strong> El value object se construye en el
 * servicio, no en el controller: es ahi donde vive la identidad
 * {@code net = gross - fee - feeTax - gmf} y donde tiene que fallar si no
 * cuadra. Un command que ya trajera el VO obligaria a la capa web a armarlo, y
 * entonces la validacion de dominio se estaria ejecutando en el binder.
 *
 * @param feeTaxAmount
 *            el impuesto de la comision, <b>aparte</b> de la comision. Si el
 *            servicio de la pasarela resulta excluido ese impuesto no es
 *            descontable y se vuelve mayor valor del gasto; sumarlo dentro de
 *            {@code feeAmount} hace imposible saberlo despues
 * @param gmfAmount
 *            el gravamen a los movimientos financieros de la salida —el cuatro
 *            por mil—. Unos 408.000 al ano que hoy no estan en ningun informe
 *            de margen
 * @param paymentCount
 *            cuantos cobros dice el lote que trae. Es lo que despues permite
 *            contrastar contra los pagos enlazados: si declara 37 y hay 36, hay
 *            un pago perdido
 */
public record RegisterGatewaySettlementCommand(String gateway, String settlementReference,
        BigDecimal grossAmount, BigDecimal feeAmount, BigDecimal feeTaxAmount, BigDecimal gmfAmount,
        BigDecimal netAmount, int paymentCount, LocalDate settledOn) {
}
