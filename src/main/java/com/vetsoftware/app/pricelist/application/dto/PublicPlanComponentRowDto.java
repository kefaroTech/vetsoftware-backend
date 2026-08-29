package com.vetsoftware.app.pricelist.application.dto;

import java.math.BigDecimal;

/**
 * Una linea de un paquete, ya resuelta contra la tarifa vigente.
 *
 * <p>
 * {@code planCode} es la llave de agrupacion: el servicio junta las lineas por
 * el codigo del paquete al que cuelgan, igual que
 * {@code GetPublicQuestionnaireService} agrupa opciones por pregunta, y por el
 * mismo motivo —dos consultas y una agrupacion en memoria, en vez de un N+1 en
 * el endpoint que sirve a gente sin autenticar—.
 *
 * <p>
 * <strong>No lleva {@code ItemType}, y no es un olvido.</strong> Ese enumerado
 * es el dominio de {@code catalogitem} y {@code pricelist} no lo importa —para
 * eso existe {@link com.vetsoftware.app.pricelist.domain.CatalogItemRef}—. El
 * discriminante ya viaja en el dato: {@code capacityUnit} no es nulo <em>si y
 * solo si</em> el articulo es {@code CAPACITY}, que es la invariante que
 * {@code CatalogItem} impone en su constructor y
 * {@code chk_catalog_items_capacity_unit} en el esquema. Asi que
 * {@link #esCapacidad()} decide en que lista de la respuesta cae la linea sin
 * cruzar ninguna frontera.
 *
 * <p>
 * {@code trialDays} llega ya filtrado por politica: la consulta lo devuelve
 * nulo salvo que el articulo sea {@code ELIGIBLE}. Hoy
 * {@code chk_catalog_items_trial_policy} ya exige que un {@code NEVER_FREE}
 * tenga los dos campos nulos, pero leer {@code default_trial_days} sin mirar la
 * elegibilidad ataria la promesa publica a una sola mitad de un arco exclusivo:
 * el dia que el CHECK se relaje, la landing empezaria a prometer pruebas que
 * nadie concedio.
 */
public record PublicPlanComponentRowDto(String planCode, String code, String name,
        String capacityUnit, int includedQuantity, Integer trialDays, BigDecimal extraUnitAmount) {

    /**
     * Un contador que se compra por unidades, frente a un modulo que se enciende.
     */
    public boolean esCapacidad() {
        return capacityUnit != null && !capacityUnit.isBlank();
    }
}
