package com.vetsoftware.app.infrastructure.observability.business;

/** Catálogo único de nombres públicos de métricas de negocio. */
public final class BusinessMetricNames {

    public static final String PREFIX = "vetsoftware.business.";

    public static final String SALES_OPERATIONS = PREFIX + "sales.operations";
    public static final String SALES_AMOUNT = PREFIX + "sales.amount";
    public static final String SALES_LINES = PREFIX + "sales.lines";

    public static final String DIAN_TRANSMISSIONS = PREFIX + "dian.transmissions";
    public static final String DIAN_TRANSMISSION_DURATION = PREFIX + "dian.transmission.duration";
    public static final String DIAN_BACKLOG = PREFIX + "dian.backlog";
    public static final String DIAN_CONTINGENCY_EXHAUSTED = PREFIX + "dian.contingency.exhausted";

    public static final String DOCUMENT_DELIVERY = PREFIX + "document.delivery";

    public static final String INVENTORY_MOVEMENTS = PREFIX + "inventory.movements";
    public static final String INVENTORY_UNITS = PREFIX + "inventory.units";
    public static final String INVENTORY_LOW_STOCK = PREFIX + "inventory.low.stock";
    public static final String INVENTORY_EXPIRING_LOTS = PREFIX + "inventory.expiring.lots";

    public static final String APPOINTMENT_TRANSITIONS = PREFIX + "appointments.transitions";

    public static final String CASH_SESSIONS = PREFIX + "cash.sessions";
    public static final String CASH_CLOSING_DIFFERENCE = PREFIX + "cash.closing.difference";

    public static final String SNAPSHOT_AGE = PREFIX + "metrics.snapshot.age";

    // ── Dinero de suscripciones (#606) ─────────────────────────────────────────
    //
    // Hasta aqui, de los ocho dominios que mueven la plata de las suscripciones
    // -contrato, cotizacion, cargos, cuentas de cobro, pagos, imputaciones,
    // cobranza y entitlements- lo unico que se emitia era el histograma de
    // latencia que produce @Observed. Un histograma de latencia no sabe decir si
    // el cargo salio, si salio duplicado, ni por cuanto.
    //
    // NINGUNA LLEVA companyId, Y NO ES NEGOCIABLE. Con 500 clinicas, una etiqueta
    // por empresa multiplica cada serie por 500 y convierte estas siete metricas
    // en varios miles, sobre un plan que ya roza su techo de series activas y
    // cuyo rebase hace que Grafana Cloud RECHACE LA INGESTA ENTERA en silencio.
    // La empresa viaja en el MDC (actor.companyId) y como atributo de span; ahi
    // es donde se responde «a quien le paso». Aqui se responde «cuanto y de que
    // clase», que es otra pregunta.
    //
    // TAMPOCO SE VIGILAN CON SLO. El volumen es de unos 500 eventos AL MES: un
    // indicador basado en tasa es estadisticamente vacio ahi -un solo fallo
    // supera cualquier umbral porcentual razonable- asi que estas siete se
    // vigilan por CONTEO ABSOLUTO, «esto deberia ser cero», y no por presupuesto
    // de error. Ver docs/SLO_VETSOFTWARE.md.
    //
    // Y NINGUNA APORTA UN VALOR NUEVO AL TAG `result`. Los seis que usan
    // -completed, cancelled, rejected, pending, failed, duplicate_ignored- ya
    // estaban declarados. Es deliberado: abrir un vocabulario paralelo para el
    // mismo concepto impide preguntar «cuantas operaciones se rechazaron hoy» a
    // traves de todo el sistema.

    /** Cargos devengados, por clase y desenlace. Contador. */
    public static final String SUBSCRIPTION_CHARGES = PREFIX + "subscription.charges";

    /** Importe devengado, por clase de cargo. Histograma, no contador. */
    public static final String SUBSCRIPTION_CHARGED_AMOUNT = PREFIX + "subscription.charged.amount";

    /** Cuentas de cobro, por estado de emision y desenlace. */
    public static final String SUBSCRIPTION_DOCUMENTS = PREFIX + "subscription.documents";

    /** Pagos registrados, por medio y desenlace. */
    public static final String SUBSCRIPTION_PAYMENTS = PREFIX + "subscription.payments";

    /** Imputaciones contra una cuenta de cobro, por clase de fuente. */
    public static final String SUBSCRIPTION_APPLICATIONS = PREFIX + "subscription.applications";

    /**
     * Transiciones de estado del contrato. Es la unica de las siete cuya cuenta
     * absoluta es una alerta por si sola: {@code to.status="read_only"} es un
     * cliente al que se le acaba de cortar la escritura.
     */
    public static final String SUBSCRIPTION_STATUS_TRANSITIONS = PREFIX
            + "subscription.status.transitions";

    /** Recalculos de entitlements, por disparador y desenlace. */
    public static final String SUBSCRIPTION_ENTITLEMENT_RECALCULATIONS = PREFIX
            + "subscription.entitlement.recalculations";

    // Alta de superadministradores de plataforma por invitacion (#360). El
    // prefijo dice "business" y esto es administracion de plataforma: es el
    // precio de heredar la lista blanca de cardinalidad, que solo actua dentro
    // de este prefijo. Fuera de el no hay ninguna barrera y un identificador
    // como etiqueta se publicaria sin mas. Un prefijo propio exigiria extender
    // el filtro a un segundo catalogo, y eso es un cambio aparte.
    public static final String SYSTEM_USER_REQUESTS = PREFIX + "system.user.requests";
    public static final String SYSTEM_USER_APPROVALS = PREFIX + "system.user.approvals";
    public static final String SYSTEM_USER_INVITATIONS = PREFIX + "system.user.invitations";
    public static final String SYSTEM_USER_PROVISIONED = PREFIX + "system.user.provisioned";

    private BusinessMetricNames() {
    }
}
