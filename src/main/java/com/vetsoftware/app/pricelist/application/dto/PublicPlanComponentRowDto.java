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
 *
 * <p>
 * <strong>Un importe por ciclo, con el ciclo en el nombre.</strong> Antes habia
 * uno solo, {@code extraUnitAmount}, y valia siempre el mensual porque el
 * {@code LEFT JOIN} estaba clavado ahi. Un nombre sin ciclo sobre un dato que
 * depende del ciclo es la forma exacta en que el defecto sobrevivio a las
 * revisiones: se leia bien. Los dos nulos son informacion, no ausencia de dato
 * —«este articulo no se vende suelto en ese ciclo»—, y en un modulo los dos son
 * irrelevantes: solo un {@code CAPACITY} se compra por unidades.
 *
 * @param monthlyExtraUnitAmount
 *            precio de la unidad adicional en el tramo de entrada del ciclo
 *            {@code MONTHLY}, o nulo si no esta tarifado ahi.
 * @param annualExtraUnitAmount
 *            lo mismo para {@code ANNUAL}. No es el mensual por doce ni por
 *            diez: es el importe propio de la fila anual, que es contra el que
 *            cotiza {@code CreateQuoteService}.
 */
public record PublicPlanComponentRowDto(String planCode, String code, String name,
        String capacityUnit, int includedQuantity, Integer trialDays,
        BigDecimal monthlyExtraUnitAmount, BigDecimal annualExtraUnitAmount) {

    /**
     * Un contador que se compra por unidades, frente a un modulo que se enciende.
     */
    public boolean esCapacidad() {
        return capacityUnit != null && !capacityUnit.isBlank();
    }
}
