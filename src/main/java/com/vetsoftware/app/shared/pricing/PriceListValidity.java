package com.vetsoftware.app.shared.pricing;

import java.time.LocalDate;

/**
 * Una ventana de vigencia y <b>el unico predicado que decide si una fecha cae
 * dentro</b>.
 *
 * <p>
 * <b>Existe para que no haya dos.</b> El predicado nacio dentro de
 * {@code quote.domain.PriceListRef} al cerrar D-73 y solo cubria la cotizacion.
 * El camino del contrato necesita exactamente la misma regla en la cabecera, y
 * la rodaja de contrato no puede importar el dominio de la cotizacion. Copiarlo
 * habria dejado dos comparaciones de fechas que nada obliga a mover juntas: el
 * dia que una cambie -un extremo que pasa a exclusivo, un nulo que se trata de
 * otra forma- el sistema empieza a cotizar con una regla y a firmar con otra, y
 * el desajuste solo se descubre cuando alguien factura de mas.
 *
 * <p>
 * <b>No sabe de tarifas, y eso es deliberado.</b> Antes llevaba dentro el id y
 * el codigo de la lista de precios, lo que la ataba por concepto a la rodaja
 * {@code pricelist} aunque no la importase por tipo. El criterio de admision
 * del kernel es explicito -lo compartido no referencia ninguna feature: ni
 * entidades, ni DTOs, ni FKs- y que {@code SIN_CRUCE_DE_DOMINIOS} exima a
 * {@code ..shared..} no vuelve correcto lo que la regla no mira: es justo el
 * mecanismo por el que un kernel se degrada en la capa horizontal que el
 * documento prohibe. Aqui solo hay dos fechas, del mismo orden que
 * {@code Money}. Quien la tenga pone el nombre.
 *
 * <p>
 * <b>{@code validTo} nulo es una ventana abierta, no un error.</b> El esquema
 * lo permite —{@code valid_to DATE} nulable con
 * {@code chk_price_lists_validity}— y la lista viva del catalogo lo tiene asi
 * ({@code 311_publish_price_list_2026} la deja con {@code valid_to = NULL}). Un
 * {@code hoy <= validTo} escrito sin pensar en el nulo descartaria justo la
 * unica tarifa publicada y tumbaria el alta de empresas entera, que es un fallo
 * mucho peor que el que se venia a corregir. Por eso el nulo se comprueba antes
 * de comparar.
 */
public record PriceListValidity(LocalDate validFrom, LocalDate validTo) {

    public PriceListValidity {
        // Espejo de price_lists.valid_from NOT NULL: una lista sin fecha de inicio no
        // es una lista sin vigencia, es una fila corrupta.
        if (validFrom == null)
            throw new IllegalArgumentException("price list validFrom is required");
        if (validTo != null && validTo.isBefore(validFrom))
            throw new IllegalArgumentException("price list validTo must not be before validFrom");
    }

    /**
     * Dentro de la ventana el dia dado: {@code validFrom <= date} y, <b>solo si hay
     * fecha de fin</b>, {@code date <= validTo}. Los dos extremos son inclusivos.
     *
     * <p>
     * La fecha llega por parametro y no se saca de {@code LocalDate.now()}: quien
     * pregunta la deriva del reloj inyectado y con la zona del negocio (D-81),
     * porque entre las 19:00 y la medianoche «hoy» en horario universal ya es
     * manana y esta comparacion decidiria distinto.
     */
    public boolean isEffectiveOn(LocalDate date) {
        if (date == null)
            throw new IllegalArgumentException("date is required to check price list validity");
        return !date.isBefore(validFrom) && (validTo == null || !date.isAfter(validTo));
    }

    /**
     * Igual que {@link #isEffectiveOn(LocalDate)} pero en negativo: si la fecha cae
     * fuera, el fallo sale con tipo propio y con la ventana dentro, no como un
     * vacio que se disfraza mas abajo de «tarifa no encontrada» o de «articulo sin
     * precio publicado».
     *
     * <p>
     * <b>Quien llama pone el id y el codigo</b>, porque este tipo no los tiene: son
     * de su rodaja. Asi el 409 sigue diciendo QUE tarifa fallo sin que el kernel
     * tenga que saber que existen las tarifas.
     */
    public void requireEffectiveOn(LocalDate date, Long priceListId, String code) {
        if (!isEffectiveOn(date))
            throw new PriceListNotEffectiveException(priceListId, code, validFrom, validTo, date);
    }
}
