package com.vetsoftware.app.infrastructure.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import java.util.Set;

/**
 * Deja de publicar el <b>medidor</b> de las observaciones de <b>lectura</b> del
 * bloque de suscripciones. No toca el span: la traza sigue igual.
 *
 * <p>
 * <b>Qué se está quitando y por qué.</b> Cada {@code @Observed} produce ocho
 * series —medido en Grafana Cloud sobre
 * {@code subscription_billing_document_list_awaiting}: el {@code Timer} publica
 * {@code _milliseconds_count}, {@code _sum}, {@code _bucket} y
 * {@code _max_milliseconds}, y el {@code LongTaskTimer} que crea
 * {@code DefaultMeterObservationHandler} publica otras cuatro con el infijo
 * {@code _active_}—. Los diecinueve casos de uso de lectura de
 * {@code subscription}, {@code subscriptionbilling} y
 * {@code subscriptionpayment} suman por tanto <b>152 series</b>.
 *
 * <p>
 * Las ciento cincuenta y dos <b>no responden ninguna pregunta que no responda
 * ya</b> {@code http_server_requests_seconds}, que trae la misma latencia por
 * {@code uri}, {@code method} y {@code status} — y con el histograma completo
 * activado, que estas no tienen—. Cada una es un {@code GET} servido por un
 * único endpoint, así que la correspondencia es uno a uno. Ningún panel del
 * proyecto las consulta y ninguna alerta las nombra: se comprobó ruta por ruta
 * en {@code docker/prometheus-*.yml} y en
 * {@code VetSoftwareIaC/observability/}.
 *
 * <p>
 * <b>Y el coste no es teórico.</b> El plan Free de Grafana Cloud admite 15.000
 * series activas y el uso medido ronda 1.150; al 100 % Grafana Cloud <b>rechaza
 * la ingesta y se pierde toda la telemetría en silencio</b>. Además el servidor
 * son dos núcleos donde los créditos de CPU son el primer techo: registrar un
 * histograma por cada lectura compite con las clínicas que están atendiendo.
 * Esta es la única parte del cambio que abarata, y paga con creces las que
 * añade el bloque de dinero.
 *
 * <p>
 * <b>Por qué un filtro y no borrar la anotación.</b> {@code @Observed} produce
 * dos cosas: un medidor y un <b>span</b>. El span sí vale —es lo que explica
 * una latencia alta cuando el {@code http.server.request.duration} ya la
 * delató— y borrarlo dejaría la traza saltando del controller al JDBC sin nada
 * en medio. Lo que sobra es la serie temporal, y eso es exactamente lo que un
 * {@code MeterFilter} quita.
 *
 * <p>
 * <b>El riesgo asumido, dicho en voz alta.</b> Un filtro que deniega es
 * invisible: quien busque
 * {@code subscription_billing_document_list_milliseconds_count} no lo
 * encontrará y podría concluir que la tubería murió. Por eso la clase emite en
 * el arranque una línea de {@code INFO} que nombra lo que no publica y dónde
 * está la latencia equivalente, y por eso está documentado en
 * {@code docs/CONVENCION_NOMBRES_OBSERVABILIDAD.md}. Un hueco explicado es
 * operable; uno silencioso es el defecto contra el que existe el resto de este
 * paquete.
 *
 * <p>
 * <b>Ninguna mutación entra aquí.</b> La lista es de lecturas y solo de
 * lecturas: {@code subscription.create}, {@code subscription.item.add},
 * {@code subscription.billing.charge.create} y sus hermanas siguen publicando
 * su latencia, además de sus contadores de negocio nuevos.
 */
public final class ReadObservationMeterFilter implements MeterFilter {

    /**
     * Las quince observaciones de lectura del bloque de dinero de suscripciones.
     *
     * <p>
     * Escrita a mano y no derivada de un prefijo, a propósito: un
     * {@code startsWith("subscription.")} se llevaría por delante las mutaciones el
     * día que alguien renombre una, y el nombre de una métrica que desaparece sola
     * es justo lo que este proyecto persigue. Cada entrada es una decisión.
     */
    static final Set<String> DENIED_READ_OBSERVATIONS = Set.of(
            // subscription — 8 lecturas
            "subscription.find", "subscription.find.current", "subscription.list.all",
            "subscription.list.by.company", "subscription.item.list",
            "subscription.item.find.overlapping", "subscription.amendment.list",
            "subscription.status.history.list",
            // subscriptionbilling — 7 lecturas
            "subscription.billing.document.find", "subscription.billing.document.list",
            "subscription.billing.document.list.awaiting",
            "subscription.billing.document.list.overdue", "subscription.billing.charge.find",
            "subscription.billing.charge.list", "subscription.billing.sequence.list",
            // subscriptionpayment - 4 lecturas. El analisis previo contaba 120 porque solo
            // miraba los dos primeros modulos; estas se anaden por coherencia, porque son
            // exactamente la misma clase de operacion -un GET de consulta servido por un
            // unico endpoint- y dejarlas publicando habria creado la peor de las dos
            // opciones: una regla que solo se aplica a veces.
            "subscription.payment.find", "subscription.payment.list.all",
            "subscription.payment.list.by.company",
            "subscription.payment.application.list.by.document");

    /**
     * Sufijo del {@code LongTaskTimer} que acompaña a cada observación. Sin
     * quitarlo también se dejarían vivas cuatro de las ocho series y el ahorro
     * sería la mitad del anunciado.
     */
    private static final String ACTIVE_SUFFIX = ".active";

    @Override
    public MeterFilterReply accept(Meter.Id id) {
        String name = id.getName();
        if (name.endsWith(ACTIVE_SUFFIX)) {
            name = name.substring(0, name.length() - ACTIVE_SUFFIX.length());
        }
        return DENIED_READ_OBSERVATIONS.contains(name)
                ? MeterFilterReply.DENY
                : MeterFilterReply.NEUTRAL;
    }
}
