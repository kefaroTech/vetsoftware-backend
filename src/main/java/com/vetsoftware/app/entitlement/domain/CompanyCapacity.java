package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDateTime;

/**
 * El techo contratado de un eje y lo que la empresa lleva usado, para poder
 * avisar antes de bloquear.
 *
 * <p>
 * <strong>El eje viene del catalogo, no de una lista cerrada.</strong> Hasta
 * BE-629 esto era un enumerado de cuatro valores, comprobado ademas en un
 * {@code CHECK} del esquema, asi que vender un limite nuevo era una migracion y
 * un despliegue. Ahora es {@link LimitDimensionRef}: una fila.
 *
 * <p>
 * <strong>No hay invariante {@code used <= limit}, y es deliberado</strong>
 * (R-LIMIT-38). Bajar de plan deja legitimamente a un cliente con 5 usuarios y
 * un techo de 3: los datos no se destruyen, se le impide crear mas. Una regla
 * que lo prohibiera haria fallar entero el recalculo que baja el techo y
 * dejaria al cliente atrapado en el estado anterior para siempre.
 *
 * <p>
 * <strong>{@code used_quantity} no se mueve nunca leyendo, modificando y
 * guardando desde Java</strong>: va con un
 * {@code UPDATE ... SET used_quantity = used_quantity + ?} atomico en el motor
 * (R-LIMIT-01). Por eso la tabla no lleva {@code version}
 * ({@code E6_YA_PROTEGIDO}): un 409 cada vez que dos usuarios se dan de alta a
 * la vez no protegeria nada que el motor no proteja ya.
 *
 * <p>
 * <strong>Dos sellos, porque son dos hechos distintos</strong> (R-ENT-13). El
 * sello del techo dice cuando se derivo el limite del contrato; el sello del
 * consumo dice cuando se comprobo por ultima vez que el contador cuadra con las
 * filas reales. Fundidos en una sola columna, el recalculo dejaba el indicador
 * fresco justo despues de no haber mirado el consumo: un indicador de salud que
 * miente es peor que no tener ninguno.
 */
public class CompanyCapacity {

    private final Long id;
    private final Long companyId;
    private final LimitDimensionRef dimension;
    private final PeriodKey periodKey;
    private final int limitQuantity;
    private final int usedQuantity;
    private final Long subscriptionId;
    private final LocalDateTime limitRecalculatedAt;
    private final LocalDateTime usageReconciledAt;
    private final LocalDateTime createdDate;

    public CompanyCapacity(Long id, Long companyId, LimitDimensionRef dimension,
            PeriodKey periodKey, int limitQuantity, int usedQuantity, Long subscriptionId,
            LocalDateTime limitRecalculatedAt, LocalDateTime usageReconciledAt,
            LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (dimension == null)
            throw new IllegalArgumentException("limit dimension is required");
        if (periodKey == null)
            throw new IllegalArgumentException(
                    "period key is required: a non-flow counter carries the sentinel,"
                            + " never an empty value");
        // R-LIMIT-05 del lado del dominio, en las dos direcciones. Espejo de
        // chk_company_capacities_period_key.
        if (dimension.measureKind().requiresPeriodKey() && !periodKey.isRealPeriod())
            throw new IllegalArgumentException("dimension " + dimension.code()
                    + " is FLOW and needs a real period key, but carries the sentinel");
        if (!dimension.measureKind().requiresPeriodKey() && periodKey.isRealPeriod())
            throw new IllegalArgumentException("dimension " + dimension.code() + " is "
                    + dimension.measureKind() + " and must carry the sentinel period key, but"
                    + " carries " + periodKey.value());
        if (limitRecalculatedAt == null)
            throw new IllegalArgumentException("limit recalculated at is required");
        // chk_company_capacities_quantities
        if (limitQuantity < 0)
            throw new IllegalArgumentException("limit quantity cannot be negative");
        if (usedQuantity < 0)
            throw new IllegalArgumentException("used quantity cannot be negative");
        this.id = id;
        this.companyId = companyId;
        this.dimension = dimension;
        this.periodKey = periodKey;
        this.limitQuantity = limitQuantity;
        this.usedQuantity = usedQuantity;
        this.subscriptionId = subscriptionId;
        this.limitRecalculatedAt = limitRecalculatedAt;
        this.usageReconciledAt = usageReconciledAt;
        this.createdDate = createdDate == null ? limitRecalculatedAt : createdDate;
    }

    /**
     * Contador recien derivado del contrato: sin id y sin consumo previo.
     *
     * <p>
     * Nace <strong>sin sello de consumo</strong>: que el techo se acabe de calcular
     * no dice nada sobre si el consumo cuadra. Ese sello lo escribe el recuento que
     * de verdad mira las filas (R-LIMIT-30), y hasta entonces vale {@code null},
     * que es la respuesta honesta a cuando se comprobo.
     */
    public static CompanyCapacity contracted(Long companyId, LimitDimensionRef dimension,
            PeriodKey periodKey, int limitQuantity, Long subscriptionId,
            LocalDateTime limitRecalculatedAt) {
        return new CompanyCapacity(null, companyId, dimension, periodKey, limitQuantity, 0,
                subscriptionId, limitRecalculatedAt, null, limitRecalculatedAt);
    }

    /**
     * El techo nuevo sobre el contador que ya existia. Conserva id, consumo, fecha
     * de creacion <strong>y el sello del consumo</strong>: el recalculo no ha
     * mirado las filas reales, asi que no tiene nada nuevo que decir sobre ellas.
     *
     * <p>
     * Esto sirve para <em>calcular</em> el estado resultante, nunca para
     * escribirlo. La escritura va por
     * {@code CompanyCapacityRepository.upsertCeiling}, que no nombra la columna del
     * consumo. Reescribir la fila entera con el {@code usedQuantity} leido aqui es
     * exactamente el defecto #648: una baja de empleado ocurrida mientras el
     * recalculo corre se pierde, y el cliente queda con un techo que no puede
     * llenar.
     */
    public CompanyCapacity reconciledFrom(CompanyCapacity existing) {
        if (existing == null)
            return this;
        return new CompanyCapacity(existing.id, companyId, dimension, periodKey, limitQuantity,
                existing.usedQuantity, subscriptionId, limitRecalculatedAt,
                existing.usageReconciledAt, existing.createdDate);
    }

    /**
     * El techo cae a cero: el eje dejo de estar contratado, el consumo se queda.
     */
    public CompanyCapacity withoutContract(Long noSubscriptionId, LocalDateTime at) {
        return new CompanyCapacity(id, companyId, dimension, periodKey, 0, usedQuantity,
                noSubscriptionId, at, usageReconciledAt, createdDate);
    }

    /** Ya no queda margen: el momento exacto de ofrecer la ampliacion. */
    public boolean isExhausted() {
        return usedQuantity >= limitQuantity;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public LimitDimensionRef getDimension() {
        return dimension;
    }

    public PeriodKey getPeriodKey() {
        return periodKey;
    }

    public int getLimitQuantity() {
        return limitQuantity;
    }

    public int getUsedQuantity() {
        return usedQuantity;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public LocalDateTime getLimitRecalculatedAt() {
        return limitRecalculatedAt;
    }

    public LocalDateTime getUsageReconciledAt() {
        return usageReconciledAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
