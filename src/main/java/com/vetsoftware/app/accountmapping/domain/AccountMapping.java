package com.vetsoftware.app.accountmapping.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El puente entre un concepto de negocio y las cuentas que mueve: qué se
 * debita, qué se acredita y —cuando el ingreso se difiere— contra qué cuenta se
 * aparca mientras tanto.
 *
 * <h2>Catalogo global: aqui no hay empresa, y es a proposito</h2>
 *
 * <p>
 * El mapeo es de los libros de Lumbre, no de la clinica.
 * {@code account_mappings} no tiene columna {@code company_id} y
 * {@code AccountMappingJpaEntity} no alcanza {@code CompanyJpaEntity} por
 * ninguna asociacion: si la alcanzara, las cuatro reglas duras de aislamiento
 * de BE-COV caerian sobre la feature entera.
 *
 * <h2>Los tres centinelas que no se ven desde Java</h2>
 *
 * <p>
 * {@code catalogItemId}, {@code chargeType} y {@code taxTreatment} son
 * nulables. <strong>En SQL dos {@code NULL} no son iguales</strong>, asi que la
 * unicidad que proponia el documento maestro —con las tres columnas dentro—
 * <b>no restringia nada</b> para nueve de las doce clases: dos mapeos de
 * {@code VAT_PAYABLE} identicos habrian entrado los dos y el asiento habria
 * tomado el primero que llegara. El changeset 343 lo cierra con tres columnas
 * generadas {@code STORED} —{@code catalog_item_key}, {@code charge_type_key},
 * {@code tax_treatment_key}, que sustituyen el vacio por {@code 0} o
 * {@code '-'}— y {@code uq_account_mappings_case} si restringe. Ninguna de las
 * tres se mapea: las calcula MySQL.
 *
 * <p>
 * {@link #catalogItemKey()}, {@link #chargeTypeKey()} y
 * {@link #taxTreatmentKey()} reproducen ese mismo calculo aqui, para que la
 * consulta de resolucion pueda comparar contra las columnas generadas sin
 * reescribir la regla dentro de un {@code @Query}.
 *
 * <h2>Un solo mapeo vigente por supuesto</h2>
 *
 * <p>
 * Lo que impide dos mapeos abiertos para el mismo supuesto no es este
 * constructor sino la cuarta columna generada, {@code current_mapping_marker}:
 * mientras el mapeo no tiene fecha de fin el marcador vale el supuesto completo
 * y {@code uq_account_mappings_current} rechaza el duplicado; en cuanto se
 * cierra, el marcador pasa a {@code NULL} y libera el hueco para su relevo.
 * Java no puede cuidar eso —dos peticiones concurrentes leerian las dos que no
 * hay ninguna abierta—, la base si.
 */
public class AccountMapping {

    private static final int MAX_MAPPING_KEY_LENGTH = 60;
    private static final int MAX_ACCOUNT_CODE_LENGTH = 10;
    private static final int MAX_REFINEMENT_LENGTH = 20;

    /** El mismo centinela que calcula {@code catalog_item_key} en la base. */
    public static final long NO_CATALOG_ITEM_KEY = 0L;

    /**
     * El mismo centinela que calculan {@code charge_type_key} y
     * {@code tax_treatment_key} en la base.
     */
    public static final String NO_REFINEMENT_KEY = "-";

    private final Long id;
    private final MappingKind mappingKind;

    /**
     * La subclave dentro de la clase: el codigo del articulo para {@code REVENUE},
     * la tarifa ({@code 19}, {@code 5}, {@code 0}) para {@code VAT_PAYABLE}, el
     * tipo y el municipio para las retenciones, el codigo del banco para
     * {@code BANK}. <strong>Nunca nula</strong>: donde no hay subclave, se escribe
     * {@code '-'}. Una columna nulable dentro de una unicidad no restringe nada.
     */
    private final String mappingKey;

    private final Long catalogItemId;
    private final String chargeType;
    private final String taxTreatment;
    private final String debitAccountCode;
    private final String creditAccountCode;
    private final String deferredAccountCode;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final LocalDateTime createdDate;
    private final boolean enabled;
    private final Long version;

    public AccountMapping(Long id, MappingKind mappingKind, String mappingKey, Long catalogItemId,
            String chargeType, String taxTreatment, String debitAccountCode,
            String creditAccountCode, String deferredAccountCode, LocalDate validFrom,
            LocalDate validTo, LocalDateTime createdDate, boolean enabled, Long version) {
        validate(mappingKind, mappingKey, catalogItemId, chargeType, taxTreatment, debitAccountCode,
                creditAccountCode, deferredAccountCode, validFrom, validTo, createdDate);
        this.id = id;
        this.mappingKind = mappingKind;
        this.mappingKey = mappingKey;
        this.catalogItemId = catalogItemId;
        this.chargeType = chargeType;
        this.taxTreatment = taxTreatment;
        this.debitAccountCode = debitAccountCode;
        this.creditAccountCode = creditAccountCode;
        this.deferredAccountCode = deferredAccountCode;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.version = version;
    }

    /**
     * Mapeo nuevo. Nace habilitado y sin version: la asigna Hibernate al insertar.
     */
    public static AccountMapping create(MappingKind mappingKind, String mappingKey,
            Long catalogItemId, String chargeType, String taxTreatment, String debitAccountCode,
            String creditAccountCode, String deferredAccountCode, LocalDate validFrom,
            LocalDate validTo, LocalDateTime createdDate) {
        return new AccountMapping(null, mappingKind, mappingKey, catalogItemId, chargeType,
                taxTreatment, debitAccountCode, creditAccountCode, deferredAccountCode, validFrom,
                validTo, createdDate, true, null);
    }

    /**
     * Cierra la vigencia poniendo la fecha de fin.
     *
     * <p>
     * <strong>Un mapeo no se edita: se cierra y se abre otro.</strong> Cambiarle la
     * cuenta en sitio reescribiria en silencio contra que cuenta se asentaron todas
     * las facturas anteriores, y no habria forma de distinguir despues las que se
     * hicieron con el mapeo viejo. Cerrar es ademas lo que libera el hueco de
     * {@code uq_account_mappings_current} para publicar el relevo.
     */
    public AccountMapping close(LocalDate closedOn) {
        if (validTo != null)
            throw new AccountMappingAlreadyClosedException(id, validTo);
        return new AccountMapping(id, mappingKind, mappingKey, catalogItemId, chargeType,
                taxTreatment, debitAccountCode, creditAccountCode, deferredAccountCode, validFrom,
                closedOn, createdDate, enabled, version);
    }

    /**
     * Si el mapeo aplica ese dia. <b>El limite superior es estricto</b>: el dia
     * escrito en {@code validTo} es el primero en que el mapeo ya no aplica, de
     * modo que el que se cierra y el que lo releva ese mismo dia no se pisan.
     */
    public boolean isEffectiveOn(LocalDate on) {
        return !validFrom.isAfter(on) && (validTo == null || validTo.isAfter(on));
    }

    /** La vigencia sigue abierta. */
    public boolean isOpen() {
        return validTo == null;
    }

    /** El mismo valor que la base calcula en {@code catalog_item_key}. */
    public long catalogItemKey() {
        return catalogItemId == null ? NO_CATALOG_ITEM_KEY : catalogItemId;
    }

    /** El mismo valor que la base calcula en {@code charge_type_key}. */
    public String chargeTypeKey() {
        return chargeType == null ? NO_REFINEMENT_KEY : chargeType;
    }

    /** El mismo valor que la base calcula en {@code tax_treatment_key}. */
    public String taxTreatmentKey() {
        return taxTreatment == null ? NO_REFINEMENT_KEY : taxTreatment;
    }

    private static void validate(MappingKind mappingKind, String mappingKey, Long catalogItemId,
            String chargeType, String taxTreatment, String debitAccountCode,
            String creditAccountCode, String deferredAccountCode, LocalDate validFrom,
            LocalDate validTo, LocalDateTime createdDate) {
        if (mappingKind == null)
            throw new IllegalArgumentException("mappingKind is required");
        validateMappingKey(mappingKey);
        validateRefinement(mappingKind, catalogItemId, chargeType, taxTreatment);
        validateAccounts(mappingKind, debitAccountCode, creditAccountCode, deferredAccountCode);
        validateValidity(validFrom, validTo);
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    /**
     * Espejo de {@code chk_account_mappings_key}. La cadena vacia es peor que el
     * nulo: entraria en la unicidad como un valor mas y abriria un segundo mapeo
     * indistinguible del que usa el centinela {@code '-'}.
     */
    private static void validateMappingKey(String mappingKey) {
        if (mappingKey == null || mappingKey.isBlank())
            throw new IllegalArgumentException("mappingKey is required");
        if (mappingKey.length() > MAX_MAPPING_KEY_LENGTH)
            throw new IllegalArgumentException("mappingKey must be 60 chars or less");
    }

    /**
     * Espejo de {@code chk_account_mappings_refine}: el afinado por articulo, tipo
     * de cargo y tratamiento fiscal solo cabe cuando el hecho <em>viene de algo
     * vendido</em>. El impuesto generado, la comision de pasarela o el banco no
     * tienen articulo, y sin esta comprobacion entrarian con uno.
     */
    private static void validateRefinement(MappingKind mappingKind, Long catalogItemId,
            String chargeType, String taxTreatment) {
        validateRefinementLength("chargeType", chargeType);
        validateRefinementLength("taxTreatment", taxTreatment);
        if (mappingKind.acceptsRefinement())
            return;
        if (catalogItemId != null || chargeType != null || taxTreatment != null)
            throw new IllegalArgumentException("catalogItemId, chargeType and taxTreatment are only"
                    + " allowed for REVENUE and DEFERRED_REVENUE mappings");
    }

    private static void validateRefinementLength(String field, String value) {
        if (value != null && (value.isBlank() || value.length() > MAX_REFINEMENT_LENGTH))
            throw new IllegalArgumentException(field + " must be 1 to 20 chars when present");
    }

    /**
     * Las dos cuentas del asiento son obligatorias; la de diferido solo cabe en
     * {@code REVENUE} y {@code DEFERRED_REVENUE}, espejo de
     * {@code chk_account_mappings_deferred}.
     */
    private static void validateAccounts(MappingKind mappingKind, String debitAccountCode,
            String creditAccountCode, String deferredAccountCode) {
        validateAccountCode("debitAccountCode", debitAccountCode, true);
        validateAccountCode("creditAccountCode", creditAccountCode, true);
        validateAccountCode("deferredAccountCode", deferredAccountCode, false);
        if (deferredAccountCode != null && !mappingKind.acceptsRefinement())
            throw new IllegalArgumentException("deferredAccountCode is only allowed for REVENUE and"
                    + " DEFERRED_REVENUE mappings");
    }

    private static void validateAccountCode(String field, String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required)
                throw new IllegalArgumentException(field + " is required");
            if (value != null)
                throw new IllegalArgumentException(field + " must not be blank when present");
            return;
        }
        if (value.length() > MAX_ACCOUNT_CODE_LENGTH)
            throw new IllegalArgumentException(field + " must be 10 chars or less");
    }

    /** Espejo de {@code chk_account_mappings_validity}. */
    private static void validateValidity(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null)
            throw new IllegalArgumentException("validFrom is required");
        if (validTo != null && !validTo.isAfter(validFrom))
            throw new IllegalArgumentException("validTo must be after validFrom");
    }

    public Long getId() {
        return id;
    }

    public MappingKind getMappingKind() {
        return mappingKind;
    }

    public String getMappingKey() {
        return mappingKey;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public String getChargeType() {
        return chargeType;
    }

    public String getTaxTreatment() {
        return taxTreatment;
    }

    public String getDebitAccountCode() {
        return debitAccountCode;
    }

    public String getCreditAccountCode() {
        return creditAccountCode;
    }

    public String getDeferredAccountCode() {
        return deferredAccountCode;
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
