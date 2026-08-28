package com.vetsoftware.app.revenuerecognitionline.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Cuanto se gano de verdad en un mes, y en que periodo contable quedo
 * registrado.
 *
 * <h2>Es un libro que solo se agrega</h2>
 *
 * <p>
 * <strong>Esta clase no tiene un solo mutador y su entidad no lleva
 * {@code @Version}</strong>, y las dos cosas son la misma decision:
 * {@code revenue_recognition_lines} figura en
 * {@code ENTIDADES_EXENTAS_DE_VERSION} con el codigo {@code E1_APPEND_ONLY}. Un
 * reconocimiento mal calculado <b>no se corrige encima</b>: se compensa con
 * otra fila de signo contrario. Por eso {@code recognizedAmount} puede ser
 * negativo y por eso {@code chk_rrl_amount} solo prohibe el cero — una fila de
 * importe cero no compensa nada y solo ensucia el libro.
 *
 * <h2>Los dos meses, que no son el mismo</h2>
 *
 * <ul>
 * <li>{@code periodKey} es el mes al que el ingreso se <b>imputa</b>: el mes en
 * que el servicio se presto.</li>
 * <li>{@code postingPeriod} es el periodo contable en que se <b>registra</b>, y
 * lleva clave foranea contra {@code accounting_periods}.</li>
 * </ul>
 *
 * <p>
 * <strong>{@code postingPeriod >= periodKey} siempre</strong>, espejo de
 * {@code chk_rrl_not_backwards}. Con formato {@code AAAA-MM} y colacion
 * {@code ascii_bin}, la comparacion lexicografica <b>es</b> la cronologica —y
 * por eso la misma comparacion vale en Java sobre {@code String}—. Un hecho
 * tardio se puede registrar en un periodo posterior o en el mismo, jamas en uno
 * anterior: eso es lo que hace que el informe de marzo siga dando lo que se
 * declaro.
 *
 * <h2>La empresa esta dentro, y de ahi salen las cuatro reglas de
 * aislamiento</h2>
 *
 * <p>
 * A diferencia del resto del bloque contable, esta tabla <b>si</b> tiene
 * {@code company_id}: el ingreso es de una clinica concreta. Eso activa las
 * cuatro reglas duras de BE-COV sobre la feature entera, y por eso todos sus
 * puertos van cerrados a {@code hasRole('SYSTEM')} —el reconocimiento de
 * ingreso es un libro de plataforma, no una pantalla de cliente— y el
 * repositorio ofrece siempre la variante acotada por empresa.
 */
public class RevenueRecognitionLine {

    /**
     * Espejo de {@code chk_rrl_period_key}, el mismo {@code REGEXP} mensual que usa
     * {@code chk_accounting_periods_key}.
     */
    private static final Pattern MONTHLY_KEY = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");

    /** {@code DECIMAL(19,2)}: un tercer decimal lo redondearia MySQL sin avisar. */
    private static final int MAX_AMOUNT_SCALE = 2;

    private final Long id;
    private final Long companyId;
    private final Long chargeId;
    private final String periodKey;
    private final String postingPeriod;
    private final BigDecimal recognizedAmount;
    private final RecognitionMethod method;
    private final LocalDateTime createdDate;

    public RevenueRecognitionLine(Long id, Long companyId, Long chargeId, String periodKey,
            String postingPeriod, BigDecimal recognizedAmount, RecognitionMethod method,
            LocalDateTime createdDate) {
        validate(companyId, chargeId, periodKey, postingPeriod, recognizedAmount, method,
                createdDate);
        this.id = id;
        this.companyId = companyId;
        this.chargeId = chargeId;
        this.periodKey = periodKey;
        this.postingPeriod = postingPeriod;
        this.recognizedAmount = recognizedAmount;
        this.method = method;
        this.createdDate = createdDate;
    }

    /**
     * Renglon nuevo. No hay {@code update} ni {@code close} que lo acompañen: este
     * libro solo se agrega.
     */
    public static RevenueRecognitionLine record(Long companyId, Long chargeId, String periodKey,
            String postingPeriod, BigDecimal recognizedAmount, RecognitionMethod method,
            LocalDateTime createdDate) {
        return new RevenueRecognitionLine(null, companyId, chargeId, periodKey, postingPeriod,
                recognizedAmount, method, createdDate);
    }

    /**
     * El renglon que compensa a este: mismo cargo y mismo mes de imputacion,
     * importe opuesto, y un periodo contable <b>distinto y posterior</b>.
     *
     * <p>
     * <strong>El periodo distinto no es un detalle de estilo: es lo que hace la
     * correccion escribible.</strong> {@code uq_rrl_recognition} es
     * {@code (company_id, charge_id, period_key, posting_period)}, asi que dos
     * filas del mismo cargo y mes solo caben si se registraron en periodos
     * contables distintos — que es la regla de negocio literal: no se corrige
     * dentro de un periodo, se compensa en el primero abierto. Compensar dentro del
     * mismo periodo choca contra la unicidad, y eso es ademas la llave
     * antiduplicados que atrapa el reintento del proceso nocturno.
     */
    public RevenueRecognitionLine offsetIn(String correctingPostingPeriod,
            LocalDateTime createdOn) {
        return new RevenueRecognitionLine(null, companyId, chargeId, periodKey,
                correctingPostingPeriod, recognizedAmount.negate(), method, createdOn);
    }

    /** {@code true} si el renglon compensa a otro: importe negativo. */
    public boolean isOffset() {
        return recognizedAmount.signum() < 0;
    }

    private static void validate(Long companyId, Long chargeId, String periodKey,
            String postingPeriod, BigDecimal recognizedAmount, RecognitionMethod method,
            LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (chargeId == null)
            throw new IllegalArgumentException("chargeId is required");
        validateMonthlyKey("periodKey", periodKey);
        validateMonthlyKey("postingPeriod", postingPeriod);
        validateNotBackwards(periodKey, postingPeriod);
        validateAmount(recognizedAmount);
        if (method == null)
            throw new IllegalArgumentException("method is required");
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    private static void validateMonthlyKey(String field, String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " is required");
        if (!MONTHLY_KEY.matcher(value).matches())
            throw new IllegalArgumentException(field + " must have the form yyyy-MM: " + value);
    }

    /**
     * Espejo de {@code chk_rrl_not_backwards}. La comparacion es de cadenas y eso
     * es <b>correcto</b>, no un atajo: con el formato {@code AAAA-MM} el orden
     * lexicografico coincide exactamente con el cronologico, que es la misma razon
     * por la que la base puede imponerlo con un {@code CHECK} sobre una columna
     * {@code ascii_bin}.
     */
    private static void validateNotBackwards(String periodKey, String postingPeriod) {
        if (postingPeriod.compareTo(periodKey) < 0)
            throw new IllegalArgumentException("postingPeriod " + postingPeriod
                    + " is before periodKey " + periodKey + ": revenue is never posted backwards");
    }

    /**
     * Espejo de {@code chk_rrl_amount} mas la escala, que la constraint no puede
     * expresar: {@code DECIMAL(19,2)} no rechaza un tercer decimal, lo
     * <em>redondea</em>, y el ingreso guardado deja de ser el calculado.
     */
    private static void validateAmount(BigDecimal recognizedAmount) {
        if (recognizedAmount == null)
            throw new IllegalArgumentException("recognizedAmount is required");
        if (recognizedAmount.signum() == 0)
            throw new IllegalArgumentException(
                    "recognizedAmount must not be zero: a zero line offsets nothing");
        if (recognizedAmount.scale() > MAX_AMOUNT_SCALE)
            throw new IllegalArgumentException("recognizedAmount must have 2 decimals or fewer");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getChargeId() {
        return chargeId;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public String getPostingPeriod() {
        return postingPeriod;
    }

    public BigDecimal getRecognizedAmount() {
        return recognizedAmount;
    }

    public RecognitionMethod getMethod() {
        return method;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
