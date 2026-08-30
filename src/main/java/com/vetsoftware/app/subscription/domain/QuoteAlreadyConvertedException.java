package com.vetsoftware.app.subscription.domain;

/**
 * Una cotizacion, un contrato. Lo garantiza {@code uq_subscriptions_quote}
 * sobre {@code subscriptions.quote_id} (changeset 391), y esta excepcion es
 * como se traduce esa violacion de unique al conflicto de negocio que de verdad
 * es.
 *
 * <p>
 * <strong>El codigo no puede comprobarlo antes y darlo por bueno.</strong>
 * {@code ReplaceSubscriptionFromQuoteService} lleva una guarda que devuelve el
 * contrato ya firmado cuando el reintento llega en serie, y eso cubre el caso
 * frecuente —el doble clic—. No cubre el de verdad peligroso: dos peticiones
 * simultaneas leen las dos «todavia no hay contrato de esta oferta» y firman
 * las dos. El indice unico es la unica autoridad, exactamente por el mismo
 * razonamiento que {@link CompanyAlreadyHasActiveSubscriptionException}, y el
 * camino correcto es intentar la escritura y traducir el rechazo.
 *
 * <p>
 * <strong>Y el indice compuesto que ya existia no lo cubria.</strong>
 * {@code (company_id, quote_id)} respalda la clave foranea a {@code quotes} y
 * empieza por la empresa: dos filas con la misma cotizacion <em>y la misma
 * empresa</em> pasan por el sin chocar, que es justo la carrera de aqui.
 *
 * <p>
 * <strong>Por que importa que el mensaje sea este y no un 500.</strong> Sin
 * traduccion, la carrera sale como una violacion de integridad generica: el
 * cliente ve un error que no puede entender ni resolver sobre una operacion que
 * <em>ya funciono</em> —su contrato existe— y soporte se pone a buscar un fallo
 * que no hay.
 *
 * <p>
 * GlobalExceptionHandler: <strong>409</strong>,
 * {@code QUOTE_ALREADY_CONVERTED}.
 */
public class QuoteAlreadyConvertedException extends RuntimeException {

    public QuoteAlreadyConvertedException(Long quoteId) {
        super("Quote already has a subscription: " + quoteId);
    }
}
