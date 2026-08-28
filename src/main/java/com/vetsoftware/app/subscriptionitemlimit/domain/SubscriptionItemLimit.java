package com.vetsoftware.app.subscriptionitemlimit.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <strong>El techo congelado el día que el cliente firmó.</strong>
 *
 * <p>
 * Existe por la misma razón por la que se congela el precio: si mañana se baja
 * el modo limitado de 100 a 80 mascotas para los clientes nuevos, quien firmó
 * con 100 debe seguir en 100. Sin esta fila, el próximo recálculo le bajaría el
 * techo <em>en silencio</em> y nadie lo vería hasta que el cliente reclamara.
 *
 * <p>
 * <strong>D-75 acota lo que sí se mueve: las mejoras del cupo de fábrica se
 * propagan a los contratos vivos; los recortes no.</strong> Por eso la fila se
 * actualiza después de nacer y por eso lleva bloqueo optimista: no es un
 * documento inmutable. Y por eso esa propagación tiene su propia operación
 * —{@link #improveFrom}— y no se hace con la tabla de excepciones negociadas,
 * que quedaría llena de filas que no negoció nadie y vaciaría de significado el
 * informe de a quién se le han hecho excepciones.
 *
 * <p>
 * Las claves foráneas son compuestas y llevan la empresa dentro: el techo de
 * una clínica no puede colgar de la línea de contrato de otra.
 */
public class SubscriptionItemLimit {

    private final Long id;
    private final Long companyId;
    private final Long subscriptionItemId;
    private final Long limitDimensionId;
    private final MeasureKind measureKind;
    private LimitMode mode;
    private Integer limitQuantity;
    private ResetPeriod resetPeriod;
    private LimitEnforcement enforcement;
    private BigDecimal overageUnitAmount;
    private int warnThreshold;
    private final LocalDateTime createdDate;
    private final boolean enabled;
    private final Long version;

    public SubscriptionItemLimit(Long id, Long companyId, Long subscriptionItemId,
            Long limitDimensionId, MeasureKind measureKind, LimitMode mode, Integer limitQuantity,
            ResetPeriod resetPeriod, LimitEnforcement enforcement, BigDecimal overageUnitAmount,
            int warnThreshold, LocalDateTime createdDate, boolean enabled, Long version) {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
        if (subscriptionItemId == null)
            throw new IllegalArgumentException("subscription item id is required");
        if (limitDimensionId == null)
            throw new IllegalArgumentException("limit dimension id is required");
        if (measureKind == null)
            throw new IllegalArgumentException("measure kind is required");
        validate(measureKind, mode, limitQuantity, resetPeriod, enforcement, overageUnitAmount,
                warnThreshold);
        this.id = id;
        this.companyId = companyId;
        this.subscriptionItemId = subscriptionItemId;
        this.limitDimensionId = limitDimensionId;
        this.measureKind = measureKind;
        this.mode = mode;
        this.limitQuantity = limitQuantity;
        this.resetPeriod = resetPeriod;
        this.enforcement = enforcement;
        this.overageUnitAmount = overageUnitAmount;
        this.warnThreshold = warnThreshold;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.version = version;
    }

    /**
     * Congela el techo de fábrica en la línea del contrato el día de la firma. A
     * partir de aquí, lo que diga el catálogo deja de importarle a este cliente
     * salvo que mejore.
     */
    public static SubscriptionItemLimit freeze(Long companyId, Long subscriptionItemId,
            Long limitDimensionId, MeasureKind measureKind, LimitMode mode, Integer limitQuantity,
            ResetPeriod resetPeriod, LimitEnforcement enforcement, BigDecimal overageUnitAmount,
            int warnThreshold, LocalDateTime createdDate) {
        return new SubscriptionItemLimit(null, companyId, subscriptionItemId, limitDimensionId,
                measureKind, mode, limitQuantity, resetPeriod, enforcement, overageUnitAmount,
                warnThreshold, createdDate, true, null);
    }

    /**
     * <strong>D-75: solo las mejoras entran.</strong> Subir el cupo de fábrica de
     * 100 a 200 llega a los contratos vivos; bajarlo de 100 a 80 no toca ninguno.
     *
     * <p>
     * «Mejora» son exactamente dos cosas: pasar de un techo a no tener ninguno, o
     * subir la cantidad. Todo lo demás —bajar la cantidad, poner techo donde no lo
     * había, o dejarlo igual— se ignora y devuelve {@code false}, que es lo que
     * permite al llamante contar cuántos contratos movió de verdad.
     *
     * @return {@code true} si la fila cambió
     */
    public boolean improveFrom(LimitMode factoryMode, Integer factoryQuantity) {
        if (factoryMode == null)
            throw new IllegalArgumentException("factory mode is required");
        if (mode == LimitMode.FULL)
            return false;
        if (factoryMode == LimitMode.FULL) {
            this.mode = LimitMode.FULL;
            this.limitQuantity = null;
            return true;
        }
        if (factoryQuantity == null || factoryQuantity <= limitQuantity)
            return false;
        this.limitQuantity = factoryQuantity;
        return true;
    }

    /**
     * Espeja las cinco restricciones {@code CHECK} de la tabla. Cada regla de aquí
     * tiene su gemela en el esquema.
     */
    private static void validate(MeasureKind measureKind, LimitMode mode, Integer limitQuantity,
            ResetPeriod resetPeriod, LimitEnforcement enforcement, BigDecimal overageUnitAmount,
            int warnThreshold) {
        if (mode == null)
            throw new IllegalArgumentException("mode is required");
        if (mode.requiresQuantity() && limitQuantity == null)
            throw new IllegalArgumentException("mode LIMITED requires a quantity");
        if (!mode.requiresQuantity() && limitQuantity != null)
            throw new IllegalArgumentException("mode FULL cannot carry a quantity");
        if (limitQuantity != null && limitQuantity < 0)
            throw new IllegalArgumentException("limit quantity cannot be negative");
        if (enforcement == null)
            throw new IllegalArgumentException("enforcement is required");
        if (measureKind.requiresResetPeriod() && resetPeriod == null)
            throw new IllegalArgumentException("reset period is required for a FLOW dimension");
        if (!measureKind.requiresResetPeriod() && resetPeriod != null)
            throw new IllegalArgumentException("reset period only applies to a FLOW dimension");
        if (enforcement.requiresOveragePrice()) {
            if (overageUnitAmount == null || overageUnitAmount.signum() <= 0)
                throw new IllegalArgumentException("OVERAGE requires a positive unit price");
            if (!measureKind.admitsOverage())
                throw new IllegalArgumentException("OVERAGE does not fit a CUMULATIVE dimension");
        } else if (overageUnitAmount != null) {
            throw new IllegalArgumentException("overage unit price only applies to OVERAGE");
        }
        if (warnThreshold < 1 || warnThreshold > 100)
            throw new IllegalArgumentException("warn threshold must be between 1 and 100");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getSubscriptionItemId() {
        return subscriptionItemId;
    }

    public Long getLimitDimensionId() {
        return limitDimensionId;
    }

    public MeasureKind getMeasureKind() {
        return measureKind;
    }

    public LimitMode getMode() {
        return mode;
    }

    public Integer getLimitQuantity() {
        return limitQuantity;
    }

    public ResetPeriod getResetPeriod() {
        return resetPeriod;
    }

    public LimitEnforcement getEnforcement() {
        return enforcement;
    }

    public BigDecimal getOverageUnitAmount() {
        return overageUnitAmount;
    }

    public int getWarnThreshold() {
        return warnThreshold;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getVersion() {
        return version;
    }
}
