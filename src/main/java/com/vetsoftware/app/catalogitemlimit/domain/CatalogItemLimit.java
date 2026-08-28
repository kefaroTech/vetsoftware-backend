package com.vetsoftware.app.catalogitemlimit.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * El techo <strong>de fábrica</strong> de un artículo sobre un eje: qué trae de
 * serie y qué trae durante la prueba, que son dos decisiones distintas.
 *
 * <p>
 * Su gemela congelada al firmar vive en otra tabla, y las dos existen por la
 * misma razón por la que se congela el precio: si mañana se baja el modo
 * limitado de 100 a 80 mascotas para los clientes nuevos, quien firmó con 100
 * debe seguir en 100. Sin la copia congelada, el próximo recálculo le bajaría
 * el techo en silencio.
 *
 * <p>
 * <strong>La prueba va sin techo por defecto</strong> (D-06), y es decisión de
 * negocio: para lo que sirve una prueba es para migrar los datos que ya se
 * tienen. Si una clínica llega con 400 mascotas y el cupo gratis son 100,
 * aplicarle el cupo desde el día 0 hace que la prueba no sirva para nada y no
 * convierta. Por eso {@code trialMode} es un campo y no una constante: se puede
 * endurecer por artículo sin desplegar nada.
 *
 * <p>
 * <strong>Sin empresa</strong>: catálogo global, igual que el artículo del que
 * cuelga.
 */
public class CatalogItemLimit {

    private final Long id;
    private final Long catalogItemId;
    private final Long limitDimensionId;
    private final MeasureKind measureKind;
    private LimitMode mode;
    private Integer limitQuantity;
    private ResetPeriod resetPeriod;
    private LimitEnforcement enforcement;
    private BigDecimal overageUnitAmount;
    private int warnThreshold;
    private LimitMode trialMode;
    private Integer trialLimitQuantity;
    private final LocalDateTime createdDate;
    private final boolean enabled;
    private final Long version;

    public CatalogItemLimit(Long id, Long catalogItemId, Long limitDimensionId,
            MeasureKind measureKind, LimitMode mode, Integer limitQuantity, ResetPeriod resetPeriod,
            LimitEnforcement enforcement, BigDecimal overageUnitAmount, int warnThreshold,
            LimitMode trialMode, Integer trialLimitQuantity, LocalDateTime createdDate,
            boolean enabled, Long version) {
        if (catalogItemId == null)
            throw new IllegalArgumentException("catalog item id is required");
        if (limitDimensionId == null)
            throw new IllegalArgumentException("limit dimension id is required");
        if (measureKind == null)
            throw new IllegalArgumentException("measure kind is required");
        validate(measureKind, mode, limitQuantity, resetPeriod, enforcement, overageUnitAmount,
                warnThreshold, trialMode, trialLimitQuantity);
        this.id = id;
        this.catalogItemId = catalogItemId;
        this.limitDimensionId = limitDimensionId;
        this.measureKind = measureKind;
        this.mode = mode;
        this.limitQuantity = limitQuantity;
        this.resetPeriod = resetPeriod;
        this.enforcement = enforcement;
        this.overageUnitAmount = overageUnitAmount;
        this.warnThreshold = warnThreshold;
        this.trialMode = trialMode;
        this.trialLimitQuantity = trialLimitQuantity;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.version = version;
    }

    /** Techo de fábrica recién declarado. */
    public static CatalogItemLimit create(Long catalogItemId, Long limitDimensionId,
            MeasureKind measureKind, LimitMode mode, Integer limitQuantity, ResetPeriod resetPeriod,
            LimitEnforcement enforcement, BigDecimal overageUnitAmount, int warnThreshold,
            LimitMode trialMode, Integer trialLimitQuantity, LocalDateTime createdDate) {
        return new CatalogItemLimit(null, catalogItemId, limitDimensionId, measureKind, mode,
                limitQuantity, resetPeriod, enforcement, overageUnitAmount, warnThreshold,
                trialMode, trialLimitQuantity, createdDate, true, null);
    }

    /**
     * Cambia el techo de fábrica.
     *
     * <p>
     * <strong>El eje y su tipo de medida no se tocan aquí.</strong> La copia de
     * {@code measure_kind} está atada por clave foránea al catálogo de ejes:
     * cambiarla sería un error del motor, y una operación que muere a mitad de
     * transacción es peor que una que no existe.
     */
    public void update(LimitMode mode, Integer limitQuantity, ResetPeriod resetPeriod,
            LimitEnforcement enforcement, BigDecimal overageUnitAmount, int warnThreshold,
            LimitMode trialMode, Integer trialLimitQuantity) {
        validate(this.measureKind, mode, limitQuantity, resetPeriod, enforcement, overageUnitAmount,
                warnThreshold, trialMode, trialLimitQuantity);
        this.mode = mode;
        this.limitQuantity = limitQuantity;
        this.resetPeriod = resetPeriod;
        this.enforcement = enforcement;
        this.overageUnitAmount = overageUnitAmount;
        this.warnThreshold = warnThreshold;
        this.trialMode = trialMode;
        this.trialLimitQuantity = trialLimitQuantity;
    }

    /**
     * Espeja las siete restricciones {@code CHECK} de la tabla. Cada regla de aquí
     * tiene su gemela en el esquema: si una de las dos cambia, cambian las dos.
     */
    private static void validate(MeasureKind measureKind, LimitMode mode, Integer limitQuantity,
            ResetPeriod resetPeriod, LimitEnforcement enforcement, BigDecimal overageUnitAmount,
            int warnThreshold, LimitMode trialMode, Integer trialLimitQuantity) {
        requireModeAndQuantity("mode", mode, limitQuantity);
        requireModeAndQuantity("trial mode", trialMode, trialLimitQuantity);
        if (enforcement == null)
            throw new IllegalArgumentException("enforcement is required");
        // chk_catalog_item_limits_reset_period
        if (measureKind.requiresResetPeriod() && resetPeriod == null)
            throw new IllegalArgumentException("reset period is required for a FLOW dimension:"
                    + " without it a monthly quota never resets");
        if (!measureKind.requiresResetPeriod() && resetPeriod != null)
            throw new IllegalArgumentException("reset period only applies to a FLOW dimension:"
                    + " on a total quota it would wipe the counter every month");
        // chk_catalog_item_limits_overage
        if (enforcement.requiresOveragePrice()) {
            if (overageUnitAmount == null || overageUnitAmount.signum() <= 0)
                throw new IllegalArgumentException("OVERAGE requires a positive unit price:"
                        + " letting through for free what should be charged is worse than"
                        + " blocking");
            if (!measureKind.admitsOverage())
                throw new IllegalArgumentException("OVERAGE does not fit a CUMULATIVE dimension:"
                        + " the customer would pay forever for records already deleted");
        } else if (overageUnitAmount != null) {
            throw new IllegalArgumentException("overage unit price only applies to OVERAGE");
        }
        // chk_catalog_item_limits_warn_threshold
        if (warnThreshold < 1 || warnThreshold > 100)
            throw new IllegalArgumentException("warn threshold must be between 1 and 100");
    }

    private static void requireModeAndQuantity(String label, LimitMode mode, Integer quantity) {
        if (mode == null)
            throw new IllegalArgumentException(label + " is required");
        if (mode.requiresQuantity() && quantity == null)
            throw new IllegalArgumentException(label + " LIMITED requires a quantity");
        if (!mode.requiresQuantity() && quantity != null)
            throw new IllegalArgumentException(label + " FULL cannot carry a quantity");
        if (quantity != null && quantity < 0)
            throw new IllegalArgumentException(label + " quantity cannot be negative");
    }

    /**
     * El techo que rige durante la prueba. Vacío = sin techo, que es el valor por
     * defecto y el que hace que la prueba sirva para migrar.
     */
    public Integer effectiveTrialLimit() {
        return trialMode.requiresQuantity() ? trialLimitQuantity : null;
    }

    public Long getId() {
        return id;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
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

    public LimitMode getTrialMode() {
        return trialMode;
    }

    public Integer getTrialLimitQuantity() {
        return trialLimitQuantity;
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
