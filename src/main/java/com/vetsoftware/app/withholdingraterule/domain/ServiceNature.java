package com.vetsoftware.app.withholdingraterule.domain;

/**
 * De que naturaleza es el servicio que se factura, que es lo que decide la
 * tarifa junto con {@link WithholdingType} y el municipio.
 *
 * <p>
 * <strong>ESTA LISTA ES COMPARTIDA Y ES LA PIEZA MAS PELIGROSA DEL
 * MODELO.</strong> Los mismos tres valores viven en
 * {@code catalog_items.service_nature} (changeset 229) y en
 * {@code chk_withholding_rate_rules_service_nature} (changeset 317), y tienen
 * que escribirse <em>identicos</em> en los tres sitios. Si divergen en un solo
 * valor —un {@code TECHNICAL_SERVICES} en plural, un {@code CONSULTANCY} por
 * {@code CONSULTING}— la busqueda de la tarifa devuelve <b>vacio</b>, la
 * retencion esperada sale <b>cero</b> y <b>NO HAY ERROR</b>: la factura se
 * emite, el cliente gira de menos porque el si retuvo, y el saldo queda abierto
 * contra alguien que pago bien. Nadie se entera hasta que se cuadra la cartera.
 *
 * <p>
 * <strong>Por que no hay un enum unico compartido entre las dos
 * features.</strong> El vertical slicing prohibe que un slice importe el
 * dominio de otro, y {@code catalogitem} guarda su columna como {@code String}
 * crudo. La defensa real no es un tipo compartido sino <em>dos</em>
 * {@code CHECK} escritos con la misma lista mas el test que fija estos
 * literales uno a uno: un renombrado silencioso rompe el test antes de romper
 * la cartera.
 */
public enum ServiceNature {

    SOFTWARE_LICENSING,

    TECHNICAL_SERVICE,

    CONSULTING
}
