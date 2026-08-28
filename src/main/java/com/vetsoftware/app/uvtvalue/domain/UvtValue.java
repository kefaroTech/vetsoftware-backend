package com.vetsoftware.app.uvtvalue.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * La Unidad de Valor Tributario de <strong>un ano concreto</strong>, con la
 * norma que la fijo.
 *
 * <p>
 * <strong>El ano es parte del dato, no un filtro.</strong> Recalcular una
 * sancion de 2025 con la UVT de 2026 no da un numero aproximado: da un numero
 * falso, y ese numero acaba en una declaracion firmada. Por eso esta clase no
 * ofrece nada parecido a «la UVT vigente» y su repositorio se consulta siempre
 * por {@code fiscalYear}: la unica forma de equivocarse de ano es pedir el ano
 * equivocado, que al menos se ve en el codigo.
 *
 * <p>
 * <strong>{@code legalReference} es obligatoria</strong> —lo es tambien en la
 * columna— porque una cifra tributaria sin la resolucion que la fijo es una
 * afirmacion sin respaldo, y quien tenga que defenderla ante la DIAN necesita
 * el numero de la resolucion, no el de la fila.
 *
 * <p>
 * Sin {@code @Version} en su entidad JPA (exenta {@code E1_APPEND_ONLY}): un
 * ano publicado no se reescribe.
 */
public class UvtValue {

    /** Espejo de {@code chk_uvt_values_year}. */
    public static final int MIN_YEAR = 2020;

    /** Espejo de {@code chk_uvt_values_year}. */
    public static final int MAX_YEAR = 2100;

    private static final int MAX_LEGAL_REFERENCE = 255;

    private final Long id;
    private final int fiscalYear;
    private final BigDecimal valueAmount;
    private final String legalReference;
    private final LocalDateTime createdDate;
    private final boolean enabled;

    public UvtValue(Long id, int fiscalYear, BigDecimal valueAmount, String legalReference,
            LocalDateTime createdDate, boolean enabled) {
        if (fiscalYear < MIN_YEAR || fiscalYear > MAX_YEAR) {
            throw new IllegalArgumentException(
                    "fiscalYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (valueAmount == null || valueAmount.signum() <= 0) {
            throw new IllegalArgumentException("valueAmount must be greater than zero");
        }
        if (legalReference == null || legalReference.isBlank()) {
            throw new IllegalArgumentException("legalReference is required");
        }
        if (legalReference.length() > MAX_LEGAL_REFERENCE) {
            throw new IllegalArgumentException(
                    "legalReference must be " + MAX_LEGAL_REFERENCE + " chars or less");
        }
        this.id = id;
        this.fiscalYear = fiscalYear;
        this.valueAmount = valueAmount;
        this.legalReference = legalReference;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static UvtValue create(int fiscalYear, BigDecimal valueAmount, String legalReference,
            LocalDateTime createdDate) {
        return new UvtValue(null, fiscalYear, valueAmount, legalReference, createdDate, true);
    }

    public Long getId() {
        return id;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public BigDecimal getValueAmount() {
        return valueAmount;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
