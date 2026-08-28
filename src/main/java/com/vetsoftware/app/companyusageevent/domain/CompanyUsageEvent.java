package com.vetsoftware.app.companyusageevent.domain;

import java.time.LocalDateTime;

/**
 * Un hecho de consumo de una empresa: <b>el registro que sostiene el cobro</b>.
 *
 * <p>
 * Es la tabla mas grande del bloque de cumplimiento —proyeccion a diez anos y
 * quinientas clinicas: unos doce millones de filas— y la unica que existe para
 * <em>ganar</em> una reclamacion: cuando un cliente discute un excedente
 * facturado, esto es lo que se le ensena, hecho a hecho, con el instante de
 * cada uno.
 *
 * <h2>La trampa que hay que leer antes de tocar nada: {@code occurredAt}</h2>
 *
 * <p>
 * <strong>{@code occurredAt} es el instante del registro consumido, NO el del
 * reloj del proceso que lo mide.</strong> La cita se agendo a las 09:14; da
 * igual que el proceso nocturno la anote a las 03:00 del dia siguiente, la fila
 * lleva las 09:14.
 *
 * <p>
 * De eso —y solo de eso— depende la unicidad
 * {@code uq_cue_fact (company_id, limit_dimension_id, usage_ref_key,
 * occurred_at)}. Si alguien rellena esta columna con {@code now()}, el
 * reintento del proceso de medicion <b>deja de chocar</b>: la segunda pasada
 * trae otro instante, entra como fila nueva, y el hecho queda duplicado. Con
 * el, el excedente facturado. Y no hay excepcion, ni log, ni fila roja: la
 * proteccion desaparece <em>en silencio</em> y lo unico que se ve es una
 * factura mas alta que nadie sabe explicar.
 *
 * <p>
 * Por eso {@code occurredAt} <b>entra por el command</b> desde quien conoce el
 * registro consumido, y el {@code Clock} inyectado del servicio se usa
 * <b>unicamente</b> para {@code createdDate}. Son dos instantes distintos a
 * proposito y no se pueden intercambiar. Los ajustes por reconteo entran como
 * hechos nuevos con su propio instante, y esos si caben.
 *
 * <h2>La rama, y por que aqui no puede estar mal</h2>
 *
 * <p>
 * El esquema tiene cuatro columnas nulables —{@code usage_owner_id},
 * {@code usage_animal_id}, {@code usage_appointment_id},
 * {@code usage_electronic_document_id}— y un {@code CHECK} de cuatro ramas que
 * exige que este poblada <em>exactamente una</em>, la que corresponde al eje.
 * Este dominio <b>no reproduce esa forma</b>: guarda {@link UsageBranch} y una
 * sola {@code usageReferenceId}, y el reparto a la columna correcta lo hace el
 * mapper. La combinacion prohibida no es que este vigilada: <b>no se puede
 * escribir</b>.
 *
 * <h2>Las otras dos invariantes</h2>
 *
 * <ul>
 * <li><b>{@code chk_cue_billable}</b>: un hecho con cargo tiene que ser
 * facturable. Cobrar por algo declarado no facturable es la contradiccion que
 * mas caro sale de defender.
 * <li><b>Sin {@code enabled}</b>: la ficha excluye expresamente los hechos de
 * uso de la marca de activo. Un hecho no se desactiva —o paso o no paso—, y una
 * prueba que se puede apagar no prueba nada.
 * </ul>
 *
 * <p>
 * <strong>Lleva {@code version}</strong> porque hay una escritura que edita la
 * fila: colgarle el cargo que la facturo. Sin bloqueo optimista, dos procesos
 * de facturacion concurrentes le colgarian dos cargos distintos al mismo hecho
 * y uno de los dos se perderia sin excepcion y sin log.
 */
public class CompanyUsageEvent {

    private final Long id;
    private final Long companyId;
    private final Long limitDimensionId;
    private final UsageBranch branch;

    /**
     * El identificador del registro consumido, en la tabla que dice
     * {@link #branch}. Va a una de las cuatro columnas de rama; cual, lo decide el
     * mapper.
     */
    private final Long usageReferenceId;

    private final LocalDateTime occurredAt;
    private final UsagePeriodKey periodKey;
    private final boolean billable;

    /** El cargo que lo facturo. Nulo mientras el hecho no se haya cobrado. */
    private final Long chargeId;

    private final LocalDateTime createdDate;
    private final Long version;

    public CompanyUsageEvent(Long id, Long companyId, Long limitDimensionId, UsageBranch branch,
            Long usageReferenceId, LocalDateTime occurredAt, UsagePeriodKey periodKey,
            boolean billable, Long chargeId, LocalDateTime createdDate, Long version) {
        validate(companyId, limitDimensionId, branch, usageReferenceId, occurredAt, periodKey,
                billable, chargeId, createdDate);
        this.id = id;
        this.companyId = companyId;
        this.limitDimensionId = limitDimensionId;
        this.branch = branch;
        this.usageReferenceId = usageReferenceId;
        this.occurredAt = occurredAt;
        this.periodKey = periodKey;
        this.billable = billable;
        this.chargeId = chargeId;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Anota un hecho de consumo. Nace <b>sin cargo</b>: medir y facturar son dos
     * operaciones distintas y la segunda ocurre mucho despues, en el cierre del
     * periodo.
     *
     * <p>
     * <strong>{@code occurredAt} lo pone quien llama, no el reloj.</strong> Ver la
     * advertencia de la clase: es la condicion de la que depende que el reintento
     * del medidor choque en vez de duplicar el cobro.
     *
     * @param occurredAt
     *            el instante <em>del registro consumido</em>
     * @param createdDate
     *            el instante en que se anota, del {@code Clock} inyectado
     */
    public static CompanyUsageEvent record(Long companyId, Long limitDimensionId,
            UsageBranch branch, Long usageReferenceId, LocalDateTime occurredAt,
            UsagePeriodKey periodKey, boolean billable, LocalDateTime createdDate) {
        return new CompanyUsageEvent(null, companyId, limitDimensionId, branch, usageReferenceId,
                occurredAt, periodKey, billable, null, createdDate, null);
    }

    /**
     * Cuelga del hecho el cargo que lo facturo.
     *
     * <p>
     * <strong>Se niega a recolgar uno ya cobrado</strong>, y esa negativa es toda
     * la barandilla que hay: la base solo exige que el cargo pertenezca a la misma
     * empresa ({@code fk_cue_charge} es compuesta) y que el hecho sea facturable,
     * no que el hueco estuviera libre. Sin esta comprobacion, una segunda pasada
     * del cierre reasignaria el hecho a otro cargo y el desglose del primero
     * dejaria de cuadrar con su importe —sin que nada fallara—.
     *
     * <p>
     * Devuelve una instancia nueva <b>conservando la version</b>: es lo que hace
     * que el {@code save} posterior sea un ciclo leer-modificar-guardar con bloqueo
     * optimista y no un insert.
     */
    public CompanyUsageEvent attachToCharge(Long newChargeId) {
        if (newChargeId == null) {
            throw new IllegalArgumentException("chargeId is required");
        }
        if (isCharged()) {
            throw new UsageEventAlreadyChargedException(id, chargeId);
        }
        return new CompanyUsageEvent(id, companyId, limitDimensionId, branch, usageReferenceId,
                occurredAt, periodKey, billable, newChargeId, createdDate, version);
    }

    /** {@code true} si el hecho ya entro en un cargo. */
    public boolean isCharged() {
        return chargeId != null;
    }

    private static void validate(Long companyId, Long limitDimensionId, UsageBranch branch,
            Long usageReferenceId, LocalDateTime occurredAt, UsagePeriodKey periodKey,
            boolean billable, Long chargeId, LocalDateTime createdDate) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }
        if (limitDimensionId == null) {
            throw new IllegalArgumentException("limitDimensionId is required");
        }
        if (branch == null) {
            throw new IllegalArgumentException("branch is required");
        }
        if (usageReferenceId == null) {
            throw new IllegalArgumentException(
                    "usageReferenceId is required: every usage fact points at the row it consumed");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        if (periodKey == null) {
            throw new IllegalArgumentException("periodKey is required");
        }
        if (createdDate == null) {
            throw new IllegalArgumentException("createdDate is required");
        }
        validateBillable(billable, chargeId);
    }

    /**
     * Espejo de {@code chk_cue_billable}. Se escribe con la condicion en el mismo
     * sentido que el motor —«hay cargo, luego es facturable»— para que las dos se
     * lean iguales al compararlas.
     */
    private static void validateBillable(boolean billable, Long chargeId) {
        if (chargeId != null && !billable) {
            throw new IllegalArgumentException("a usage event with a charge must be billable:"
                    + " charging for something recorded as non-billable is the contradiction"
                    + " that is hardest to defend in front of the customer who is disputing it");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getLimitDimensionId() {
        return limitDimensionId;
    }

    public UsageBranch getBranch() {
        return branch;
    }

    public Long getUsageReferenceId() {
        return usageReferenceId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public UsagePeriodKey getPeriodKey() {
        return periodKey;
    }

    public boolean isBillable() {
        return billable;
    }

    public Long getChargeId() {
        return chargeId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
