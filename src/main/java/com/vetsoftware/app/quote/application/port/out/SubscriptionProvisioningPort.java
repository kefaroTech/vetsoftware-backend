package com.vetsoftware.app.quote.application.port.out;

/**
 * <strong>El eslabon entre el embudo comercial y el contrato, visto desde esta
 * rodaja (DC-2).</strong> Aceptar una cotizacion deja de ser solo un cambio de
 * estado: produce el contrato que describe.
 *
 * <p>
 * <strong>Por que un puerto y no una llamada directa.</strong> El vertical
 * slicing prohibe que {@code quote.application} conozca nada de
 * {@code subscription}. Esta interfaz declara <em>que</em> hace falta —«de esta
 * oferta aceptada nace un contrato»— y el adaptador de
 * {@code infrastructure/orchestration} es el unico fichero que sabe
 * <em>quien</em> lo hace. Es el mismo patron con el que {@code registration}
 * acuña el primer contrato de una empresa ({@code InitialSubscriptionCreator} /
 * {@code PlatformCatalogSubscriptionCreator}).
 *
 * <p>
 * <strong>Se invoca DENTRO de la transaccion de la aceptacion, y eso es el
 * requisito, no un detalle.</strong> Si el contrato se creara despues, en otra
 * transaccion, existiria una ventana —corta, pero real— en la que la cotizacion
 * consta aceptada y no hay contrato detras: el cliente ha firmado y el sistema
 * no le ha dado nada. Un fallo en esa ventana la vuelve permanente y solo se
 * arregla a mano. Compartir la transaccion convierte los dos desenlaces
 * posibles en «aceptada y contratada» o «ni lo uno ni lo otro».
 *
 * <p>
 * <strong>Consecuencia para quien lo implemente: no puede tragarse
 * excepciones.</strong> Un fallo al provisionar tiene que propagarse para que
 * la aceptacion revierta con el. Un adaptador «resiliente» que registrara el
 * error y siguiera adelante reintroduciria exactamente la ventana que este
 * diseño cierra, y ademas la haria silenciosa.
 */
public interface SubscriptionProvisioningPort {

    /**
     * Provisiona el contrato de una cotizacion recien aceptada.
     *
     * @param quoteId
     *            la oferta, ya en {@code ACCEPTED} y guardada
     * @param companyId
     *            la empresa que firma. Nunca nulo: quien llama comprueba antes que
     *            la cotizacion tenga empresa, porque una oferta a un prospecto que
     *            todavia no es cliente no tiene donde poner un contrato
     */
    void provisionFromAcceptedQuote(Long quoteId, Long companyId);
}
