package com.vetsoftware.app.pricelist.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * La tarifa oficial, con fecha. Subir precios no es editar: es publicar una
 * lista nueva.
 *
 * <p>
 * <strong>No es multi-tenant.</strong> Es la tarifa global de la plataforma:
 * ninguna de sus columnas es {@code company_id} y ninguna asociacion de esta
 * feature alcanza {@code CompanyJpaEntity}. Sus puertos de entrada van cerrados
 * a {@code hasRole('SYSTEM')} a secas, que es como se satisface
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}.
 */
public class PriceList {

    private static final int CODE_MAX = 50;
    private static final int NAME_MAX = 120;
    private static final int CURRENCY_LENGTH = 3;

    private final Long id;

    /**
     * Clave de negocio estable. No se edita ni siquiera en borrador: es lo que se
     * cita en una cotizacion y en un contrato.
     */
    private final String code;

    private String name;
    private String currency;
    private LocalDate validFrom;
    private LocalDate validTo;
    private PriceListStatus status;
    private LocalDateTime publishedAt;
    private Long publishedBySystemUserId;
    private final LocalDateTime createdDate;
    private final Long version;
    private boolean enabled;

    public PriceList(Long id, String code, String name, String currency, LocalDate validFrom,
            LocalDate validTo, PriceListStatus status, LocalDateTime publishedAt,
            Long publishedBySystemUserId, LocalDateTime createdDate, Long version,
            boolean enabled) {
        validateIdentity(code, name, currency);
        validateValidity(validFrom, validTo);
        validateSignature(status, publishedAt, publishedBySystemUserId);
        this.id = id;
        this.code = code;
        this.name = name;
        this.currency = currency;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = status;
        this.publishedAt = publishedAt;
        this.publishedBySystemUserId = publishedBySystemUserId;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    /** Nace siempre en borrador y sin firma. */
    public static PriceList create(String code, String name, String currency, LocalDate validFrom,
            LocalDate validTo, LocalDateTime createdDate) {
        return new PriceList(null, code, name, currency, validFrom, validTo, PriceListStatus.DRAFT,
                null, null, createdDate, null, true);
    }

    /**
     * Cambia lo comercial de la lista. El codigo queda fuera a proposito: es la
     * clave de negocio.
     *
     * @throws PriceListNotEditableException
     *             si la lista ya no esta en DRAFT (regla R9)
     */
    public void update(String name, String currency, LocalDate validFrom, LocalDate validTo) {
        requireDraft();
        validateIdentity(this.code, name, currency);
        validateValidity(validFrom, validTo);
        this.name = name;
        this.currency = currency;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    /**
     * Congela la lista y le pone firma. A partir de aqui ni ella ni sus precios
     * admiten un solo cambio.
     */
    public void publish(Long systemUserId, LocalDateTime publishedAt) {
        if (status != PriceListStatus.DRAFT)
            throw new InvalidPriceListTransitionException(status, PriceListStatus.PUBLISHED);
        if (systemUserId == null)
            throw new IllegalArgumentException("publishedBySystemUserId is required to publish");
        if (publishedAt == null)
            throw new IllegalArgumentException("publishedAt is required to publish");
        this.status = PriceListStatus.PUBLISHED;
        this.publishedBySystemUserId = systemUserId;
        this.publishedAt = publishedAt;
    }

    /**
     * Retira la lista de circulacion conservandola consultable. Es la unica
     * mutacion admitida sobre una lista publicada, y solo mueve el estado: el
     * contenido comercial -importes, vigencias, moneda- sigue congelado.
     */
    public void archive() {
        if (status != PriceListStatus.PUBLISHED)
            throw new InvalidPriceListTransitionException(status, PriceListStatus.ARCHIVED);
        this.status = PriceListStatus.ARCHIVED;
    }

    /**
     * El guardian de R9, y el unico sitio del slice donde se decide si algo se
     * puede tocar. Lo llaman {@link #update} y los casos de uso que escriben
     * {@code catalog_prices}, que cuelgan de esta lista y por tanto heredan su
     * inmutabilidad.
     */
    public void requireDraft() {
        if (status != PriceListStatus.DRAFT)
            throw new PriceListNotEditableException(id, status);
    }

    public boolean isDraft() {
        return status == PriceListStatus.DRAFT;
    }

    private static void validateIdentity(String code, String name, String currency) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > CODE_MAX)
            throw new IllegalArgumentException("code must be " + CODE_MAX + " chars or less");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > NAME_MAX)
            throw new IllegalArgumentException("name must be " + NAME_MAX + " chars or less");
        // Espejo de chk_price_lists_currency: tres letras y en mayusculas. Se valida y
        // no se normaliza en silencio, porque cop y COP significan lo mismo para un
        // humano y distinto para una constraint que compara con UPPER().
        if (currency == null || currency.length() != CURRENCY_LENGTH)
            throw new IllegalArgumentException(
                    "currency must be " + CURRENCY_LENGTH + " chars (ISO 4217)");
        if (!currency.equals(currency.toUpperCase(Locale.ROOT)))
            throw new IllegalArgumentException("currency must be uppercase");
    }

    private static void validateValidity(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null)
            throw new IllegalArgumentException("validFrom is required");
        if (validTo != null && validTo.isBefore(validFrom))
            throw new IllegalArgumentException("validTo must not be before validFrom");
    }

    /**
     * Espejo de {@code chk_price_lists_published}: quien publica y cuando es un
     * dato obligatorio, no aspiracional. Una lista no puede salir de DRAFT sin
     * firma, ni quedarse en DRAFT con una firma puesta.
     */
    private static void validateSignature(PriceListStatus status, LocalDateTime publishedAt,
            Long publishedBySystemUserId) {
        if (status == null)
            throw new IllegalArgumentException("status is required");
        boolean signed = publishedAt != null && publishedBySystemUserId != null;
        boolean unsigned = publishedAt == null && publishedBySystemUserId == null;
        if (status == PriceListStatus.DRAFT && !unsigned)
            throw new IllegalArgumentException("a DRAFT price list must not be signed");
        if (status != PriceListStatus.DRAFT && !signed)
            throw new IllegalArgumentException(
                    "a " + status + " price list requires publishedAt and publishedBySystemUserId");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Baja logica. Una lista publicada no se da de baja: se archiva. */
    public void disable() {
        requireDraft();
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public PriceListStatus getStatus() {
        return status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Long getPublishedBySystemUserId() {
        return publishedBySystemUserId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
