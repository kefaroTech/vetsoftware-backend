package com.vetsoftware.app.billingdocumentstatushistory.domain;

import java.time.LocalDateTime;

/**
 * Un fotograma de la pelicula de un documento de cobro: cambio de un estado a
 * otro, en tal momento, movido por alguien y por un motivo escrito.
 *
 * <p>
 * <strong>Solo se agrega, y esa es la propiedad que sostiene la ficha
 * entera.</strong> No hay {@code update}, no hay {@code delete}, no hay
 * {@code enabled} y no hay reactivacion — ni aqui ni en el puerto de salida,
 * que no declara ninguna escritura sobre una fila existente. Un cambio de
 * estado ya ocurrido no deja de haber ocurrido porque alguien se arrepienta: se
 * corrige moviendo el documento otra vez, y las dos filas quedan. Es una de las
 * tablas irreemplazables del modelo —no se puede reconstruir preguntandole a un
 * tercero ni recontando— y una fila que se pudiera desactivar no probaria nada.
 *
 * <p>
 * <strong>Sin bloqueo optimista y sin {@code version}, por lo mismo.</strong>
 * {@code @Version} protege el ciclo leer-modificar-guardar, y aqui no hay
 * modificacion que proteger: dos operarios registrando a la vez producen dos
 * hechos distintos, no una carrera. Ver la exencion {@code E1_APPEND_ONLY} de
 * {@code ENTIDADES_EXENTAS_DE_VERSION}.
 *
 * <p>
 * <strong>{@code fromStatus} distinto de {@code toStatus}, comprobado
 * aqui</strong> y no en el controller ni en el servicio: es una verdad de la
 * transicion y vale aunque nadie llame al endpoint. Espejo de
 * {@code chk_bdsh_transition}; ver {@link SameStatusTransitionException}.
 *
 * <p>
 * <strong>{@code actor} es texto y no una clave foranea, a proposito.</strong>
 * Quien mueve un documento puede ser una persona con nombre o el proceso
 * automatico de facturacion, y un proceso no tiene fila en
 * {@code system_users}. Obligarlo a tenerla habria significado inventar un
 * usuario tecnico, que es justo lo que otras columnas de este modelo existen
 * para evitar. El precio es que el actor no se puede unir contra nada, y por
 * eso el otro campo obligatorio es el motivo.
 *
 * <p>
 * <strong>{@code reason} obligatorio y legible</strong> —«Factura externa
 * FE-1043 registrada», «anulado por nota credito NC-77»—. Un cambio de estado
 * sin motivo no explica nada seis meses despues, que es exactamente cuando se
 * lee esta tabla.
 */
public class BillingDocumentStatusHistory {

    /** Espejo de {@code actor VARCHAR(120)}. */
    private static final int MAX_ACTOR_LENGTH = 120;

    /** Espejo de {@code reason VARCHAR(255)}. */
    private static final int MAX_REASON_LENGTH = 255;

    private final Long id;
    private final Long companyId;

    /** El documento de cobro cuya pelicula se esta contando. */
    private final Long billingDocumentId;

    /** De donde venia. Nunca nulo: un documento siempre estaba en algo. */
    private final BillingDocumentStatus fromStatus;

    /** A donde fue. Nunca igual al anterior. */
    private final BillingDocumentStatus toStatus;

    /**
     * Cuando ocurrio el cambio. Es la columna por la que se ordena la pelicula y
     * por la que se corta a una fecha, y va en el indice
     * {@code ix_bdsh_document (company_id, billing_document_id, occurred_at)}.
     */
    private final LocalDateTime occurredAt;

    /** Persona o proceso. Ver el javadoc de la clase. */
    private final String actor;

    private final String reason;

    private final LocalDateTime createdDate;

    public BillingDocumentStatusHistory(Long id, Long companyId, Long billingDocumentId,
            BillingDocumentStatus fromStatus, BillingDocumentStatus toStatus,
            LocalDateTime occurredAt, String actor, String reason, LocalDateTime createdDate) {
        validate(companyId, billingDocumentId, fromStatus, toStatus, occurredAt, actor, reason);
        this.id = id;
        this.companyId = companyId;
        this.billingDocumentId = billingDocumentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.occurredAt = occurredAt;
        this.actor = actor;
        this.reason = reason;
        this.createdDate = createdDate;
    }

    /**
     * Un cambio de estado recien ocurrido.
     *
     * <p>
     * <strong>No hay factoria de correccion ni metodo de mutacion</strong>, y su
     * ausencia es la decision: la unica escritura que esta feature admite es anadir
     * otro fotograma.
     */
    public static BillingDocumentStatusHistory register(Long companyId, Long billingDocumentId,
            BillingDocumentStatus fromStatus, BillingDocumentStatus toStatus,
            LocalDateTime occurredAt, String actor, String reason, LocalDateTime createdDate) {
        return new BillingDocumentStatusHistory(null, companyId, billingDocumentId, fromStatus,
                toStatus, occurredAt, actor, reason, createdDate);
    }

    /**
     * Si este fotograma deja al documento esperando la factura externa. Es el
     * estado que la consulta de vigilancia cuenta a una fecha de corte.
     */
    public boolean leavesAwaitingExternal() {
        return toStatus == BillingDocumentStatus.AWAITING_EXTERNAL;
    }

    private static void validate(Long companyId, Long billingDocumentId,
            BillingDocumentStatus fromStatus, BillingDocumentStatus toStatus,
            LocalDateTime occurredAt, String actor, String reason) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (billingDocumentId == null)
            throw new IllegalArgumentException("billingDocumentId is required");
        validateTransition(billingDocumentId, fromStatus, toStatus);
        if (occurredAt == null)
            throw new IllegalArgumentException("occurredAt is required");
        validateActor(actor);
        validateReason(reason);
    }

    /**
     * Espejo de {@code chk_bdsh_statuses} —los dos extremos existen— y de
     * {@code chk_bdsh_transition} —y son distintos—.
     *
     * <p>
     * El estado de origen es obligatorio y no admite nulo como dato que se
     * desconoce: un documento siempre estaba en algo antes de moverse, y un origen
     * vacio dejaria el tramo anterior de la pelicula sin empalmar con el siguiente.
     */
    private static void validateTransition(Long billingDocumentId, BillingDocumentStatus fromStatus,
            BillingDocumentStatus toStatus) {
        if (fromStatus == null)
            throw new IllegalArgumentException("fromStatus is required");
        if (toStatus == null)
            throw new IllegalArgumentException("toStatus is required");
        if (fromStatus == toStatus)
            throw new SameStatusTransitionException(billingDocumentId, toStatus);
    }

    private static void validateActor(String actor) {
        if (actor == null || actor.isBlank())
            throw new IllegalArgumentException("actor is required");
        if (actor.length() > MAX_ACTOR_LENGTH)
            throw new IllegalArgumentException(
                    "actor must be " + MAX_ACTOR_LENGTH + " chars or less");
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("reason is required");
        if (reason.length() > MAX_REASON_LENGTH)
            throw new IllegalArgumentException(
                    "reason must be " + MAX_REASON_LENGTH + " chars or less");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public BillingDocumentStatus getFromStatus() {
        return fromStatus;
    }

    public BillingDocumentStatus getToStatus() {
        return toStatus;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
