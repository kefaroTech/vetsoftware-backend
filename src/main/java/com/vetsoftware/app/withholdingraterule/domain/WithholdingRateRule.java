package com.vetsoftware.app.withholdingraterule.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Que retencion esperar de cada cliente: la tarifa que aplica a un supuesto
 * —tipo de retencion, naturaleza del servicio y municipio— durante una
 * vigencia.
 *
 * <p>
 * Sin ella cada retencion llega como una sorpresa que deja la factura sin
 * saldar y arranca el reloj de la mora contra alguien que pago bien. Con ella
 * se sabe de antemano que de 213.010 se van a girar 205.850 y el saldo cierra
 * solo.
 *
 * <h2>Catalogo global: aqui no hay empresa, y es a proposito</h2>
 *
 * <p>
 * La tarifa depende del tipo de retencion, de la naturaleza del servicio y del
 * municipio —<b>no del cliente</b>—; lo que depende del cliente es si es agente
 * de retencion, y eso vive en {@code company_billing_profiles}. Por eso no hay
 * {@code companyId} en esta clase ni columna {@code company_id} en la tabla, y
 * por eso {@code WithholdingRateRuleJpaEntity} no alcanza
 * {@code CompanyJpaEntity} por ninguna asociacion: si la alcanzara, las cuatro
 * reglas duras de aislamiento de BE-COV caerian sobre la feature entera.
 *
 * <h2>La tarifa es un PORCENTAJE, no una fraccion</h2>
 *
 * <p>
 * El nombre lleva la unidad dentro ({@code ratePercent}) porque el campo que no
 * dice en que unidad esta se lee mal la mitad de las veces. El ICA de Bogota
 * son <b>6,9 por mil</b>, que aqui se escribe {@code 0.690000} y no
 * {@code 6.900000} ni {@code 0.0069}. La columna guarda seis decimales por lo
 * mismo: un 4,14 por mil —{@code 0.414000}— con dos decimales se corta a
 * {@code 0.41} y se retiene casi un uno por ciento de menos en cada factura,
 * calculado en silencio y sin un solo error. Por eso el constructor rechaza una
 * escala mayor que seis en vez de dejar que MySQL redondee callando.
 *
 * <h2>El municipio y el centinela que no se ve desde Java</h2>
 *
 * <p>
 * {@code municipalityCode} es obligatorio si y solo si el tipo es
 * {@link WithholdingType#ICA} y esta prohibido en los otros dos —espejo de
 * {@code chk_withholding_rate_rules_municipality}—. El dominio guarda
 * {@code null} para las nacionales; el centinela vive en la columna generada
 * {@code municipality_key}, que <b>no se mapea</b>.
 *
 * <p>
 * <strong>Ese centinela existe porque en SQL dos {@code NULL} no son
 * iguales.</strong> Dos tarifas nacionales para el mismo servicio y la misma
 * fecha —una al 3,5 y otra al 4— con el municipio vacio <b>no chocan</b> en un
 * indice unico: la base admitiria las dos, la consulta devolveria dos filas
 * para la misma vigencia y el codigo se quedaria con la primera que llegara.
 * Sustituyendo el vacio por el centinela en una columna generada, la unicidad
 * {@code uq_withholding_rate_rules_case} si restringe, y
 * {@code municipality_code} puede seguir siendo nulable para llevar clave
 * foranea de verdad contra {@code cities.dane_code}.
 *
 * <h2>La base minima va en pesos Y en unidades</h2>
 *
 * <p>
 * Al menos uno de {@code minimumBaseAmount} (pesos) y {@code minimumBaseUvt}
 * (unidades de valor tributario) tiene que estar escrito, y ninguno de los dos
 * es negativo si viene. Las dos unidades conviven porque <b>el numero en pesos
 * envejece cada ano y el numero en UVT no</b>; y la regla lleva vigencia en vez
 * de ser una constante del codigo porque el propio umbral cambia —son dos UVT,
 * no cuatro, desde 2026— y el cambio esta discutido en tribunales.
 *
 * <h2>La vigencia y las dos reglas abiertas</h2>
 *
 * <p>
 * {@code validTo} es nulo (regla abierta) o estrictamente posterior a
 * {@code validFrom}. Lo que impide <b>dos vigencias abiertas solapadas para el
 * mismo supuesto</b> no es este constructor sino la otra columna generada,
 * {@code current_rule_marker}: mientras la regla no tiene fecha de fin el
 * marcador vale el supuesto completo y
 * {@code uq_withholding_rate_rules_current} rechaza el duplicado; en cuanto se
 * cierra, el marcador pasa a {@code NULL} y libera el hueco para la siguiente.
 * Java no puede cuidar eso —dos peticiones concurrentes leerian las dos que no
 * hay ninguna abierta—, la base si.
 */
public class WithholdingRateRule {

    private static final int MAX_LEGAL_REFERENCE_LENGTH = 255;
    private static final int MUNICIPALITY_CODE_LENGTH = 5;

    /**
     * {@code DECIMAL(9,6)}. Una escala mayor no cabe: MySQL la redondearia sin
     * avisar y la tarifa guardada dejaria de ser la que alguien escribio.
     */
    private static final int MAX_RATE_SCALE = 6;

    /** Espejo de {@code chk_withholding_rate_rules_rate}. */
    private static final BigDecimal MAX_RATE_PERCENT = new BigDecimal("100");

    /**
     * El mismo centinela que calcula {@code municipality_key} en la base. Vive
     * tambien aqui para que la consulta de resolucion pueda comparar contra esa
     * columna generada sin reescribir la regla dentro de un {@code @Query}.
     */
    public static final String NATIONAL_MUNICIPALITY_KEY = "-";

    private final Long id;
    private final WithholdingType withholdingType;
    private final ServiceNature serviceNature;

    /** Codigo DIVIPOLA de cinco digitos. Solo y siempre para {@code ICA}. */
    private final String municipalityCode;

    private final BigDecimal ratePercent;
    private final BigDecimal minimumBaseAmount;
    private final BigDecimal minimumBaseUvt;
    private final String legalReference;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final LocalDateTime createdDate;
    private final boolean enabled;
    private final Long version;

    public WithholdingRateRule(Long id, WithholdingType withholdingType,
            ServiceNature serviceNature, String municipalityCode, BigDecimal ratePercent,
            BigDecimal minimumBaseAmount, BigDecimal minimumBaseUvt, String legalReference,
            LocalDate validFrom, LocalDate validTo, LocalDateTime createdDate, boolean enabled,
            Long version) {
        validate(withholdingType, serviceNature, municipalityCode, ratePercent, minimumBaseAmount,
                minimumBaseUvt, legalReference, validFrom, validTo, createdDate);
        this.id = id;
        this.withholdingType = withholdingType;
        this.serviceNature = serviceNature;
        this.municipalityCode = municipalityCode;
        this.ratePercent = ratePercent;
        this.minimumBaseAmount = minimumBaseAmount;
        this.minimumBaseUvt = minimumBaseUvt;
        this.legalReference = legalReference;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.version = version;
    }

    /**
     * Tarifa nueva. Nace habilitada y sin version: la asigna Hibernate al insertar.
     *
     * <p>
     * Acepta {@code validTo} porque cargar el historico es un caso real —una tarifa
     * que dejo de aplicarse en 2024 entra ya cerrada—, no solo abrir la vigente.
     */
    public static WithholdingRateRule create(WithholdingType withholdingType,
            ServiceNature serviceNature, String municipalityCode, BigDecimal ratePercent,
            BigDecimal minimumBaseAmount, BigDecimal minimumBaseUvt, String legalReference,
            LocalDate validFrom, LocalDate validTo, LocalDateTime createdDate) {
        return new WithholdingRateRule(null, withholdingType, serviceNature, municipalityCode,
                ratePercent, minimumBaseAmount, minimumBaseUvt, legalReference, validFrom, validTo,
                createdDate, true, null);
    }

    /**
     * Cierra la vigencia poniendo la fecha de fin.
     *
     * <p>
     * <strong>Se niega a cerrar lo que ya estaba cerrado</strong>, y esa negativa
     * es toda la barandilla que hay: la base no la pone, porque
     * {@code current_rule_marker} vale {@code NULL} en una regla cerrada y una
     * unicidad sobre columna nula no restringe nada. Sin esta comprobacion, el
     * segundo cierre machacaria en silencio la fecha desde la que la tarifa dejo de
     * aplicarse.
     *
     * <p>
     * Devuelve una instancia nueva —la clase no tiene mutadores— <b>conservando la
     * version</b>: es lo que permite que el {@code save} posterior siga siendo un
     * ciclo leer-modificar-guardar con bloqueo optimista y no un insert.
     */
    public WithholdingRateRule close(LocalDate closedOn) {
        if (validTo != null)
            throw new WithholdingRateRuleAlreadyClosedException(id, validTo);
        return new WithholdingRateRule(id, withholdingType, serviceNature, municipalityCode,
                ratePercent, minimumBaseAmount, minimumBaseUvt, legalReference, validFrom, closedOn,
                createdDate, enabled, version);
    }

    /**
     * Si la regla aplica en esa fecha: {@code validFrom <= on} y la vigencia sigue
     * abierta o termina despues.
     *
     * <p>
     * <strong>El limite superior es estricto y el inferior no.</strong> El dia
     * escrito en {@code validTo} es el primero en que la tarifa <em>ya no</em>
     * aplica, de modo que la regla que se cierra el 1 de enero y la que empieza ese
     * mismo dia se relevan sin dejar hueco ni pisarse. Un {@code >=} aqui haria que
     * las dos aplicaran a la vez durante un dia.
     */
    public boolean isEffectiveOn(LocalDate on) {
        return !validFrom.isAfter(on) && (validTo == null || validTo.isAfter(on));
    }

    /** La regla sigue abierta: no tiene fecha de fin. */
    public boolean isOpen() {
        return validTo == null;
    }

    /**
     * El mismo valor que la base calcula en {@code municipality_key}: el codigo del
     * municipio, o el centinela cuando la retencion es nacional.
     */
    public String municipalityKey() {
        return municipalityCode == null ? NATIONAL_MUNICIPALITY_KEY : municipalityCode;
    }

    private static void validate(WithholdingType withholdingType, ServiceNature serviceNature,
            String municipalityCode, BigDecimal ratePercent, BigDecimal minimumBaseAmount,
            BigDecimal minimumBaseUvt, String legalReference, LocalDate validFrom,
            LocalDate validTo, LocalDateTime createdDate) {
        if (withholdingType == null)
            throw new IllegalArgumentException("withholdingType is required");
        if (serviceNature == null)
            throw new IllegalArgumentException("serviceNature is required");
        validateMunicipality(withholdingType, municipalityCode);
        validateRate(ratePercent);
        validateMinimumBase(minimumBaseAmount, minimumBaseUvt);
        if (legalReference != null && legalReference.length() > MAX_LEGAL_REFERENCE_LENGTH)
            throw new IllegalArgumentException("legalReference must be 255 chars or less");
        validateValidity(validFrom, validTo);
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    /**
     * Espejo de {@code chk_withholding_rate_rules_municipality}. El «si y solo si»
     * son las dos mitades: sin la segunda, una retencion nacional podria llevar
     * municipio y habria dos filas para el mismo supuesto nacional —una con
     * municipio y otra sin el— que la unicidad no veria como iguales.
     */
    private static void validateMunicipality(WithholdingType withholdingType,
            String municipalityCode) {
        if (withholdingType == WithholdingType.ICA) {
            if (municipalityCode == null || municipalityCode.isBlank())
                throw new IllegalArgumentException("municipalityCode is required for ICA");
            if (municipalityCode.length() != MUNICIPALITY_CODE_LENGTH)
                throw new IllegalArgumentException("municipalityCode must be 5 characters");
            return;
        }
        if (municipalityCode != null)
            throw new IllegalArgumentException(
                    "municipalityCode must be absent unless the withholding type is ICA");
    }

    /**
     * Espejo de {@code chk_withholding_rate_rules_rate} mas la escala, que la
     * constraint no puede expresar: {@code DECIMAL(9,6)} no rechaza un septimo
     * decimal, lo <em>redondea</em>, y la tarifa guardada deja de ser la escrita.
     */
    private static void validateRate(BigDecimal ratePercent) {
        if (ratePercent == null)
            throw new IllegalArgumentException("ratePercent is required");
        if (ratePercent.signum() <= 0)
            throw new IllegalArgumentException("ratePercent must be greater than zero");
        if (ratePercent.compareTo(MAX_RATE_PERCENT) > 0)
            throw new IllegalArgumentException("ratePercent must not exceed 100");
        if (ratePercent.scale() > MAX_RATE_SCALE)
            throw new IllegalArgumentException("ratePercent must have 6 decimals or fewer");
    }

    /**
     * Espejo de {@code chk_withholding_rate_rules_minimum_base} y de
     * {@code chk_withholding_rate_rules_minimum_base_sign}. Cero es valido —hay
     * supuestos que retienen desde el primer peso— y por eso la comprobacion es
     * {@code < 0} y no {@code <= 0}.
     */
    private static void validateMinimumBase(BigDecimal minimumBaseAmount,
            BigDecimal minimumBaseUvt) {
        if (minimumBaseAmount == null && minimumBaseUvt == null)
            throw new IllegalArgumentException(
                    "at least one of minimumBaseAmount or minimumBaseUvt is required");
        if (minimumBaseAmount != null && minimumBaseAmount.signum() < 0)
            throw new IllegalArgumentException("minimumBaseAmount must not be negative");
        if (minimumBaseUvt != null && minimumBaseUvt.signum() < 0)
            throw new IllegalArgumentException("minimumBaseUvt must not be negative");
    }

    /** Espejo de {@code chk_withholding_rate_rules_validity}. */
    private static void validateValidity(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null)
            throw new IllegalArgumentException("validFrom is required");
        if (validTo != null && !validTo.isAfter(validFrom))
            throw new IllegalArgumentException("validTo must be after validFrom");
    }

    public Long getId() {
        return id;
    }

    public WithholdingType getWithholdingType() {
        return withholdingType;
    }

    public ServiceNature getServiceNature() {
        return serviceNature;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public BigDecimal getMinimumBaseAmount() {
        return minimumBaseAmount;
    }

    public BigDecimal getMinimumBaseUvt() {
        return minimumBaseUvt;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
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
