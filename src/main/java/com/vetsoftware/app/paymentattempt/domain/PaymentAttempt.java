package com.vetsoftware.app.paymentattempt.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Un cobro que <strong>no salio</strong>.
 *
 * <p>
 * {@code subscription_payments} guarda los pagos que ocurrieron. Con la
 * pasarela como unica via, cada renovacion es un cobro automatico que puede
 * rebotar, y un rechazo no tenia donde vivir: sin codigo, sin numero de intento
 * y sin nada que programara el siguiente. Meterlo en la tabla de pagos es lo
 * que ya se descarto una vez, cuando se saco de alli la devolucion por la misma
 * razon: <strong>esa tabla no se reescribe</strong>.
 *
 * <p>
 * <strong>Referencias ajenas por id escalar, no por companion VO.</strong> Esta
 * feature no lee un solo campo del documento de cobro ni del medio de pago: el
 * importe y la pasarela los trae el propio intento. Es el caso que
 * {@code CLAUDE.md} resuelve con "solo el ID + ValidationPort"; un {@code Ref}
 * que no transportara mas que el id seria peso muerto y ataria este slice a la
 * forma de otro.
 *
 * <p>
 * <strong>El codigo crudo de la pasarela se guarda, y no se ensena.</strong>
 * {@link #getGatewayDeclineCode()} queda tal cual lo devolvio la pasarela para
 * poder revisar despues la traduccion -las pasarelas cambian su catalogo y una
 * traduccion hecha hoy envejece-. Al cliente solo se le da su <em>clase</em>
 * ({@link #getDeclineKind()}), y eso lo materializa la frontera HTTP, no esta
 * clase.
 */
public class PaymentAttempt {

    /**
     * Intentos imputables al cliente dentro de {@link #RETRY_WINDOW}. Es el techo
     * que fijan las redes de tarjetas para no acosar una tarjeta que rebota.
     */
    public static final int MAX_SOFT_ATTEMPTS = 4;

    /** Ventana en la que se cuentan esos cuatro intentos: dos semanas. */
    public static final Duration RETRY_WINDOW = Duration.ofDays(14);

    private static final int MAX_GATEWAY_LENGTH = 40;
    private static final int MAX_DECLINE_CODE_LENGTH = 50;

    private final Long id;
    private final Long companyId;
    private final Long billingDocumentId;

    /**
     * Con que se intento cobrar. Admite vacio: un intento puede rebotar antes de
     * llegar a usar medio alguno -credencial mal puesta, pasarela caida-, que es
     * justamente el caso {@link DeclineKind#CONFIGURATION}.
     */
    private final Long paymentMethodId;

    private final int attemptNumber;
    private final String gateway;
    private final BigDecimal requestedAmount;

    /** Crudo, tal como lo devolvio la pasarela. Nunca sale por HTTP al tenant. */
    private final String gatewayDeclineCode;

    private final DeclineKind declineKind;
    private final LocalDateTime attemptedAt;

    /** Lo unico que muta: un intento se <strong>reprograma</strong>. */
    private LocalDateTime nextAttemptAt;

    private final LocalDateTime createdDate;
    private final Long version;

    public PaymentAttempt(Long id, Long companyId, Long billingDocumentId, Long paymentMethodId,
            int attemptNumber, String gateway, BigDecimal requestedAmount,
            String gatewayDeclineCode, DeclineKind declineKind, LocalDateTime attemptedAt,
            LocalDateTime nextAttemptAt, LocalDateTime createdDate, Long version) {
        validate(companyId, billingDocumentId, attemptNumber, gateway, requestedAmount,
                gatewayDeclineCode, declineKind, attemptedAt, nextAttemptAt);
        this.id = id;
        this.companyId = companyId;
        this.billingDocumentId = billingDocumentId;
        this.paymentMethodId = paymentMethodId;
        this.attemptNumber = attemptNumber;
        this.gateway = gateway;
        this.requestedAmount = requestedAmount;
        this.gatewayDeclineCode = gatewayDeclineCode;
        this.declineKind = declineKind;
        this.attemptedAt = attemptedAt;
        this.nextAttemptAt = nextAttemptAt;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Intento recien rebotado. El numero lo calcula el caso de uso dentro de la
     * transaccion, porque {@code uq_payment_attempts_number} lo quiere consecutivo
     * y sin huecos por documento.
     */
    public static PaymentAttempt attempted(Long companyId, Long billingDocumentId,
            Long paymentMethodId, int attemptNumber, String gateway, BigDecimal requestedAmount,
            String gatewayDeclineCode, DeclineKind declineKind, LocalDateTime attemptedAt,
            LocalDateTime nextAttemptAt, LocalDateTime createdDate) {
        return new PaymentAttempt(null, companyId, billingDocumentId, paymentMethodId,
                attemptNumber, gateway, requestedAmount, gatewayDeclineCode, declineKind,
                attemptedAt, nextAttemptAt, createdDate, null);
    }

    /**
     * Programa el siguiente reintento.
     *
     * <p>
     * Sobre un {@link DeclineKind#HARD} lanza
     * {@link HardDeclineCannotBeRetriedException} (409) en vez de dejar que la
     * violacion de {@code chk_payment_attempts_hard_has_no_retry} llegue a la cara
     * del operador como un error de integridad sin mensaje.
     */
    public void reschedule(LocalDateTime next) {
        if (next == null)
            throw new IllegalArgumentException("nextAttemptAt is required");
        if (declineKind == DeclineKind.HARD)
            throw new HardDeclineCannotBeRetriedException(id);
        if (!next.isAfter(attemptedAt))
            throw new IllegalArgumentException("nextAttemptAt must be after attemptedAt");
        this.nextAttemptAt = next;
    }

    /**
     * Si este intento gasta presupuesto del cliente.
     *
     * <p>
     * Un {@link DeclineKind#CONFIGURATION} es un fallo <strong>propio</strong>
     * -credencial mal puesta, pasarela caida, moneda no soportada- y <strong>no
     * consume los intentos del cliente ni arranca cobranza contra el</strong>.
     * Contarlo seria quemar contra alguien que no ha hecho nada mal los intentos
     * que la red permite, y las redes multan eso.
     */
    public boolean consumesCustomerAttempts() {
        return declineKind != DeclineKind.CONFIGURATION;
    }

    /** Un rechazo duro no se reintenta jamas: se pide medio de pago nuevo. */
    public boolean isRetriable() {
        return declineKind != DeclineKind.HARD;
    }

    private static void validate(Long companyId, Long billingDocumentId, int attemptNumber,
            String gateway, BigDecimal requestedAmount, String gatewayDeclineCode,
            DeclineKind declineKind, LocalDateTime attemptedAt, LocalDateTime nextAttemptAt) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (billingDocumentId == null)
            throw new IllegalArgumentException("billingDocumentId is required");
        // Espejo de chk_payment_attempts_number.
        if (attemptNumber <= 0)
            throw new IllegalArgumentException("attemptNumber must be greater than zero");
        if (gateway == null || gateway.isBlank())
            throw new IllegalArgumentException("gateway is required");
        if (gateway.length() > MAX_GATEWAY_LENGTH)
            throw new IllegalArgumentException("gateway must be 40 chars or less");
        // Espejo de chk_payment_attempts_amount.
        if (requestedAmount == null)
            throw new IllegalArgumentException("requestedAmount is required");
        if (requestedAmount.signum() <= 0)
            throw new IllegalArgumentException("requestedAmount must be greater than zero");
        if (declineKind == null)
            throw new IllegalArgumentException("declineKind is required");
        // Espejo de chk_payment_attempts_declined_by_gateway: solo el fallo propio
        // puede no traer codigo, porque en el la pasarela no llego a decidir nada.
        if (declineKind != DeclineKind.CONFIGURATION
                && (gatewayDeclineCode == null || gatewayDeclineCode.isBlank()))
            throw new IllegalArgumentException(
                    "gatewayDeclineCode is required unless the decline is CONFIGURATION");
        if (gatewayDeclineCode != null && gatewayDeclineCode.length() > MAX_DECLINE_CODE_LENGTH)
            throw new IllegalArgumentException("gatewayDeclineCode must be 50 chars or less");
        if (attemptedAt == null)
            throw new IllegalArgumentException("attemptedAt is required");
        // Espejo de chk_payment_attempts_hard_has_no_retry: en un rechazo duro el
        // vacio ES la regla, no un descuido. No hay siguiente hasta que aparezca
        // otra tarjeta.
        if (declineKind == DeclineKind.HARD && nextAttemptAt != null)
            throw new IllegalArgumentException("a HARD decline cannot schedule a next attempt");
        // Espejo de chk_payment_attempts_retry_is_later.
        if (nextAttemptAt != null && !nextAttemptAt.isAfter(attemptedAt))
            throw new IllegalArgumentException("nextAttemptAt must be after attemptedAt");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public String getGateway() {
        return gateway;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public String getGatewayDeclineCode() {
        return gatewayDeclineCode;
    }

    public DeclineKind getDeclineKind() {
        return declineKind;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
