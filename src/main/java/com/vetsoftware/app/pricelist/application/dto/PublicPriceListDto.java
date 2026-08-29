package com.vetsoftware.app.pricelist.application.dto;

import java.time.LocalDate;

/**
 * La tarifa publicada, vista por el read model publico: identidad minima y su
 * ventana de vigencia.
 *
 * <p>
 * <strong>El {@code id} no sale de la aplicacion.</strong> Lo necesita
 * {@code GetPublicPlansService} para pedir las filas de esa lista y ahi se
 * queda: la respuesta REST no lo lleva. Publicarlo diria cuantas tarifas hay y
 * daria una llave que un anonimo podria probar contra los endpoints de
 * administracion.
 *
 * <p>
 * {@code validTo} viaja hasta aqui porque la vigencia se decide en el servicio
 * con {@link com.vetsoftware.app.shared.pricing.PriceListValidity} —el unico
 * predicado del arbol— y no en el SQL. Tampoco sale: con la fecha de caducidad
 * publicada, un comprador espera al ultimo dia de la oferta.
 */
public record PublicPriceListDto(Long id, String currency, LocalDate validFrom, LocalDate validTo) {
}
