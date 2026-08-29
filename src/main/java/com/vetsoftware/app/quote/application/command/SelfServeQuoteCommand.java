package com.vetsoftware.app.quote.application.command;

import java.util.List;

/**
 * Una clinica pide su propia oferta.
 *
 * <p>
 * <strong>El tipo es la seguridad.</strong> Compare la lista de componentes con
 * la de {@link CreateQuoteCommand}: alli viajan {@code priceListId},
 * {@code validUntil}, {@code trialDays} y el descuento de cada linea, que son
 * <em>los terminos del negocio</em>. Aqui no estan. No estan validados, ni
 * ignorados, ni forzados a un valor por defecto: <strong>no se pueden
 * escribir</strong>, porque el record no los declara. Un {@code @PreAuthorize}
 * no puede mirar dentro de un cuerpo, asi que la unica forma de garantizar que
 * el precio lo pone el servidor es que el cliente no tenga donde ponerlo.
 *
 * @param companyId
 *            la empresa que contrata. Lo inyecta el controller desde el
 *            principal y el puerto lo revalida con
 *            {@code @authz.isMyCompany(#command.companyId)}; NUNCA viaja en el
 *            cuerpo REST.
 * @param billingCycle
 *            {@code MONTHLY} o {@code ANNUAL}. Es lo unico economico que el
 *            cliente decide, y no fija ningun importe: cada ciclo lleva <em>su
 *            propio</em> precio en la tarifa —el anual no es un descuento
 *            calculado sobre el mensual—, asi que elegir ciclo es elegir que
 *            columna del catalogo se lee, no cuanto cuesta.
 * @param clientRequestId
 *            llave de idempotencia que genera el cliente. Es lo que hace que un
 *            doble clic en «Confirmar» no cree dos ofertas.
 */
public record SelfServeQuoteCommand(String clientRequestId, Long companyId, String billingCycle,
        List<SelfServeQuoteLineCommand> lines) {
}
