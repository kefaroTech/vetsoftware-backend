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

    private BusinessMetricNames() {
    }
}
