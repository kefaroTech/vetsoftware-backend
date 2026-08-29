package com.vetsoftware.app.pricelist.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que ve la landing: la moneda, desde cuando rigen estos precios, y los
 * planes.
 *
 * <p>
 * {@code priceValidFrom} sale y {@code validTo} <strong>no</strong>. La
 * asimetria es deliberada: la fecha de inicio da contexto —«precios de agosto»—
 * y la de fin es informacion de negociacion. Con la caducidad publicada, quien
 * compara espera al ultimo dia de la oferta.
 *
 * <p>
 * Tampoco salen el id, el codigo, el estado, quien la publico ni cuando: eso
 * diria cuantas tarifas hay y quien las firma.
 *
 * <p>
 * Sin tarifa vigente, {@code currency} y {@code priceValidFrom} son nulos y
 * {@code plans} viene vacio. No es un error: es «hoy no hay precio publicado»,
 * y tumbar la portada por eso seria peor que servirla sin precios.
 */
public record PublicPlanCatalogDto(String currency, LocalDate priceValidFrom,
        List<PublicPlanDto> plans) {
}
