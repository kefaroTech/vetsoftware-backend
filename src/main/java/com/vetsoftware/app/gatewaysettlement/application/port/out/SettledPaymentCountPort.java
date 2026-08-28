package com.vetsoftware.app.gatewaysettlement.application.port.out;

/**
 * Cuantos cobros estan atados a un lote, contados en la tabla donde viven los
 * cobros.
 *
 * <p>
 * <strong>Devuelve un numero y nunca una lista, y esa firma es una decision de
 * seguridad, no de rendimiento.</strong> Los cobros de un lote pertenecen a
 * decenas de empresas distintas; un puerto que devolviera las filas pondria al
 * alcance de esta rodaja —que es toda de plataforma y no acota por empresa— el
 * detalle de quien cobro y cuanto. Con un {@code long} no hay nada que se pueda
 * filtrar por descuido a una respuesta. Lo que el operario necesita para
 * trabajar es saber si cuadra: la busqueda del pago perdido se hace desde el
 * lado del cobro, que si esta acotado por empresa.
 *
 * <p>
 * <strong>El par (pasarela, referencia) y no el id del lote</strong>: la clave
 * hacia atras que cose el cobro con su lote es la compuesta
 * {@code subscription_payments (gateway, settlement_reference)} contra
 * {@code uq_gateway_settlements_reference}. En {@code subscription_payments} no
 * hay ninguna columna con el id del lote, asi que contar por id exigiria un
 * salto mas.
 */
public interface SettledPaymentCountPort {

    long countByGatewayAndSettlementReference(String gateway, String settlementReference);
}
