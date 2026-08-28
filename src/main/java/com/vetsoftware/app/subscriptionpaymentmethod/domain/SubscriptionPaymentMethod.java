package com.vetsoftware.app.subscriptionpaymentmethod.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * La autorizacion para cobrarle a una clinica.
 *
 * <p>
 * El resto del modelo registra pagos que <em>ya ocurrieron</em>, pero no con
 * que se cobra ni si el cliente sigue autorizando que se le cobre. La
 * consecuencia de esa ausencia es doble y las dos son caras: una tarjeta
 * vencida y una domiciliacion que el cliente revoco en su banco se ven
 * <strong>exactamente igual</strong> que un impago voluntario, y las dos
 * arrancan la cobranza contra alguien que no ha hecho nada mal.
 *
 * <p>
 * <strong>Una revocacion no es una mora.</strong> Es el fin del mandato: la ley
 * exige autorizacion expresa —el silencio no vale— y permite revocar el debito
 * automatico en cualquier momento y sin justificar. El proceso de cobranza
 * tiene que poder distinguir las dos cosas, y {@link MandateStatus} es como las
 * distingue.
 *
 * <p>
 * <strong>NUNCA EL NUMERO DE LA TARJETA.</strong> Esta clase no tiene, y no
 * puede tener, un campo con el PAN. Lo unico que guarda es el {@code token} que
 * devuelve la pasarela —un testigo que sin la pasarela no vale nada— mas
 * {@code brand} y {@code lastFour}, que es lo justo para que el cliente
 * reconozca cual es y para avisarle <em>antes</em> de que se venza, en vez de
 * que lo descubra con el cobro rechazado. Anadir aqui el numero completo no
 * seria una funcionalidad de mas: cambiaria el regimen legal entero al que esta
 * sometido este backend.
 *
 * <p>
 * <strong>El mandato es lo que hay que poder probar</strong>, y por eso
 * {@code mandateEvidence} es obligatorio: sin constancia, la autorizacion es
 * una afirmacion propia.
 */
public class SubscriptionPaymentMethod {

    private static final int MAX_GATEWAY_LENGTH = 40;
    private static final int MAX_TOKEN_LENGTH = 255;
    private static final int MAX_BRAND_LENGTH = 30;
    private static final int MAX_EVIDENCE_LENGTH = 255;
    private static final int MAX_REVOKED_REASON_LENGTH = 255;

    /** Espejo de {@code chk_subscription_payment_methods_last_four}. */
    private static final Pattern LAST_FOUR = Pattern.compile("^[0-9]{4}$");

    private final Long id;
    private final Long companyId;
    private final PaymentMethodKind methodKind;

    /** Pasarela que emitio el testigo. Con el token forma la unicidad global. */
    private final String gateway;

    /**
     * El testigo de la pasarela. <strong>Jamas el numero de la tarjeta.</strong>
     */
    private final String token;

    private final String brand;
    private final String lastFour;
    private final LocalDate expiresOn;

    private final String mandateEvidence;
    private final LocalDateTime authorizedAt;

    private MandateStatus mandateStatus;
    private LocalDateTime revokedAt;
    private String revokedReason;
    private boolean defaultMethod;

    private final LocalDateTime createdDate;
    private final boolean enabled;
    private final Long version;

    public SubscriptionPaymentMethod(Long id, Long companyId, PaymentMethodKind methodKind,
            String gateway, String token, String brand, String lastFour, LocalDate expiresOn,
            MandateStatus mandateStatus, String mandateEvidence, LocalDateTime authorizedAt,
            LocalDateTime revokedAt, String revokedReason, boolean defaultMethod,
            LocalDateTime createdDate, boolean enabled, Long version) {
        validate(companyId, methodKind, gateway, token, brand, lastFour, expiresOn, mandateStatus,
                mandateEvidence, authorizedAt, revokedAt, revokedReason);
        this.id = id;
        this.companyId = companyId;
        this.methodKind = methodKind;
        this.gateway = gateway;
        this.token = token;
        this.brand = brand;
        this.lastFour = lastFour;
        this.expiresOn = expiresOn;
        this.mandateStatus = mandateStatus;
        this.mandateEvidence = mandateEvidence;
        this.authorizedAt = authorizedAt;
        this.revokedAt = revokedAt;
        this.revokedReason = revokedReason;
        this.defaultMethod = defaultMethod;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.version = version;
    }

    /**
     * Medio recien autorizado por el cliente. Nace {@code ACTIVE} y
     * <strong>no</strong> nace por defecto: marcarlo es una decision aparte, porque
     * solo puede haber uno por empresa y quitarselo al anterior es parte de la
     * misma operacion.
     */
    public static SubscriptionPaymentMethod register(Long companyId, PaymentMethodKind methodKind,
            String gateway, String token, String brand, String lastFour, LocalDate expiresOn,
            String mandateEvidence, LocalDateTime authorizedAt, LocalDateTime createdDate) {
        return new SubscriptionPaymentMethod(null, companyId, methodKind, gateway, token, brand,
                lastFour, expiresOn, MandateStatus.ACTIVE, mandateEvidence, authorizedAt, null,
                null, false, createdDate, true, null);
    }

    /**
     * El cliente retira la autorizacion. Guarda <strong>cuando</strong> y
     * <strong>por que</strong>, que son los dos datos con los que despues se
     * demuestra que a partir de esa fecha no habia mandato — y por tanto que los
     * cobros posteriores no debieron intentarse.
     *
     * <p>
     * <strong>No toca {@code defaultMethod}, y es deliberado.</strong> La columna
     * generada {@code default_marker} solo proyecta la empresa cuando el mandato
     * sigue {@code ACTIVE}, asi que revocar <em>libera el hueco del predeterminado
     * automaticamente</em> sin borrar el rastro de cual lo era. Justo lo que hace
     * falta el dia que el cliente cambia de tarjeta.
     */
    public void revoke(String reason, LocalDateTime at) {
        if (mandateStatus == MandateStatus.REVOKED)
            throw new MandateAlreadyRevokedException(id);
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("revoked reason is required");
        if (reason.length() > MAX_REVOKED_REASON_LENGTH)
            throw new IllegalArgumentException("revoked reason must be 255 chars or less");
        if (at == null)
            throw new IllegalArgumentException("revokedAt is required");
        if (at.isBefore(authorizedAt))
            throw new IllegalArgumentException("revokedAt cannot be before authorizedAt");
        this.mandateStatus = MandateStatus.REVOKED;
        this.revokedAt = at;
        this.revokedReason = reason;
    }

    /**
     * La tarjeta caduco. Idempotente sobre un mandato ya caducado —la fecha no la
     * pone esta operacion sino el calendario, asi que repetirla no reescribe nada—,
     * pero <strong>nunca sobre uno revocado</strong>: la revocacion es un acto del
     * cliente con fecha propia, y taparla con una caducidad borraria la unica
     * prueba de que ejercio un derecho.
     */
    public void markExpired() {
        if (mandateStatus == MandateStatus.REVOKED)
            throw new MandateAlreadyRevokedException(id);
        this.mandateStatus = MandateStatus.EXPIRED;
    }

    /**
     * Marca este medio como el predeterminado de su empresa.
     *
     * <p>
     * Solo un mandato {@code ACTIVE} puede serlo. No es una comprobacion
     * decorativa: {@code default_marker} deja de proyectar la empresa en cuanto el
     * mandato deja de estar activo, asi que marcar como predeterminado un medio
     * revocado guardaria un {@code is_default = true} que <em>no significa
     * nada</em> — la empresa se quedaria sin predeterminado y la fila afirmando que
     * lo tiene.
     */
    public void makeDefault() {
        if (mandateStatus != MandateStatus.ACTIVE)
            throw new IllegalArgumentException(
                    "only an ACTIVE mandate can be the default payment method");
        this.defaultMethod = true;
    }

    /** Deja de ser el predeterminado. Libera el hueco para otro medio. */
    public void clearDefault() {
        this.defaultMethod = false;
    }

    /** Solo un mandato vivo autoriza un cobro. */
    public boolean authorizesCharge() {
        return mandateStatus == MandateStatus.ACTIVE;
    }

    /**
     * Si la tarjeta ya caduco en la fecha dada. Un PSE nunca caduca, asi que
     * siempre responde {@code false}.
     */
    public boolean isExpiredOn(LocalDate date) {
        if (date == null)
            throw new IllegalArgumentException("date is required");
        return expiresOn != null && expiresOn.isBefore(date);
    }

    private static void validate(Long companyId, PaymentMethodKind methodKind, String gateway,
            String token, String brand, String lastFour, LocalDate expiresOn,
            MandateStatus mandateStatus, String mandateEvidence, LocalDateTime authorizedAt,
            LocalDateTime revokedAt, String revokedReason) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (methodKind == null)
            throw new IllegalArgumentException("methodKind is required");
        if (gateway == null || gateway.isBlank())
            throw new IllegalArgumentException("gateway is required");
        if (gateway.length() > MAX_GATEWAY_LENGTH)
            throw new IllegalArgumentException("gateway must be 40 chars or less");
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("token is required");
        if (token.length() > MAX_TOKEN_LENGTH)
            throw new IllegalArgumentException("token must be 255 chars or less");
        validateShape(methodKind, brand, lastFour, expiresOn);
        if (mandateStatus == null)
            throw new IllegalArgumentException("mandateStatus is required");
        // Sin constancia, la autorizacion es una afirmacion propia.
        if (mandateEvidence == null || mandateEvidence.isBlank())
            throw new IllegalArgumentException("mandateEvidence is required");
        if (mandateEvidence.length() > MAX_EVIDENCE_LENGTH)
            throw new IllegalArgumentException("mandateEvidence must be 255 chars or less");
        if (authorizedAt == null)
            throw new IllegalArgumentException("authorizedAt is required");
        validateRevocation(mandateStatus, authorizedAt, revokedAt, revokedReason);
    }

    /**
     * Espejo de {@code chk_subscription_payment_methods_card_shape} y de
     * {@code chk_subscription_payment_methods_last_four}: la tarjeta lleva los tres
     * datos y el PSE ninguno. Una columna que unos rellenan y otros no es
     * exactamente lo que el CHECK existe para impedir, y un debito PSE no caduca.
     */
    private static void validateShape(PaymentMethodKind methodKind, String brand, String lastFour,
            LocalDate expiresOn) {
        if (methodKind == PaymentMethodKind.CARD) {
            if (brand == null || brand.isBlank())
                throw new IllegalArgumentException("brand is required for a CARD payment method");
            if (brand.length() > MAX_BRAND_LENGTH)
                throw new IllegalArgumentException("brand must be 30 chars or less");
            if (lastFour == null)
                throw new IllegalArgumentException(
                        "lastFour is required for a CARD payment method");
            if (expiresOn == null)
                throw new IllegalArgumentException(
                        "expiresOn is required for a CARD payment method");
        } else if (brand != null || lastFour != null || expiresOn != null) {
            throw new IllegalArgumentException(
                    "a PSE payment method cannot carry brand, lastFour or expiresOn");
        }
        if (lastFour != null && !LAST_FOUR.matcher(lastFour).matches())
            throw new IllegalArgumentException("lastFour must be exactly 4 digits");
    }

    /** Espejo de {@code chk_subscription_payment_methods_revocation}. */
    private static void validateRevocation(MandateStatus mandateStatus, LocalDateTime authorizedAt,
            LocalDateTime revokedAt, String revokedReason) {
        if (mandateStatus == MandateStatus.REVOKED) {
            if (revokedAt == null || revokedReason == null || revokedReason.isBlank())
                throw new IllegalArgumentException(
                        "a REVOKED mandate requires both revokedAt and revokedReason");
            if (revokedAt.isBefore(authorizedAt))
                throw new IllegalArgumentException("revokedAt cannot be before authorizedAt");
            if (revokedReason.length() > MAX_REVOKED_REASON_LENGTH)
                throw new IllegalArgumentException("revoked reason must be 255 chars or less");
        } else if (revokedAt != null || revokedReason != null) {
            throw new IllegalArgumentException(
                    "only a REVOKED mandate can carry revokedAt or revokedReason");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public PaymentMethodKind getMethodKind() {
        return methodKind;
    }

    public String getGateway() {
        return gateway;
    }

    public String getToken() {
        return token;
    }

    public String getBrand() {
        return brand;
    }

    public String getLastFour() {
        return lastFour;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public MandateStatus getMandateStatus() {
        return mandateStatus;
    }

    public String getMandateEvidence() {
        return mandateEvidence;
    }

    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public boolean isDefaultMethod() {
        return defaultMethod;
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
