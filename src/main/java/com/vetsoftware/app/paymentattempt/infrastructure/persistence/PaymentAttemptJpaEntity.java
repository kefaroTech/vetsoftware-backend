package com.vetsoftware.app.paymentattempt.infrastructure.persistence;

import com.vetsoftware.app.paymentattempt.domain.DeclineKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code payment_attempts}.
 *
 * <p>
 * <strong>Sin una sola asociacion {@code @ManyToOne}, y es una
 * decision.</strong> Las dos claves foraneas de esta tabla son
 * <em>compuestas</em> {@code (company_id, id)}, y mapearlas obligaria a un
 * {@code @JoinColumns} que comparte la columna {@code company_id} con el
 * escalar que la escribe. Hibernate exige que todas las columnas de una
 * propiedad compartan modo de escritura, asi que la unica combinacion valida
 * seria escalares escribibles mas asociaciones {@code insertable = false} — la
 * trampa que documenta {@code BillingDocumentApplicationJpaEntity}, donde el
 * fallo no es de la clase sino del {@code entityManagerFactory} y se lleva por
 * delante la aplicacion entera sin senalar aqui—. Este slice <em>no lee ningun
 * campo</em> del documento ni del medio de pago, asi que la asociacion no
 * compraria nada: los ids escalares bastan, las referencias se validan por
 * puerto acotado, y sin asociacion no hay N+1 que evitar ni
 * {@code @EntityGraph} que mantener.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: un intento se <em>reprograma</em>
 * ({@code ReschedulePaymentAttemptService}), que es una segunda escritura
 * declarada. Eximirlo seria una exencion que miente.
 *
 * <p>
 * <strong>Sin {@code enabled}, y por tanto sin {@code @SQLDelete} ni
 * {@code @SQLRestriction}.</strong> Es una bitacora probatoria: es lo que
 * sostiene "se intento cuatro veces" antes de degradar a nadie, y una prueba
 * que se puede desactivar no prueba nada.
 */
@Entity
@Table(name = "payment_attempts")
public class PaymentAttemptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Escalar y no una asociacion a {@code CompanyJpaEntity}: es la forma que usan
     * las otras tablas de dinero del bloque ({@code subscription_payments},
     * {@code subscription_billing_documents}) y la mitad de las dos claves foraneas
     * compuestas de esta.
     */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "billing_document_id", nullable = false)
    private Long billingDocumentId;

    /** Admite vacio: un fallo de configuracion rebota sin llegar a usar medio. */
    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "gateway", nullable = false, length = 40)
    private String gateway;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal requestedAmount;

    /**
     * Crudo, tal como lo devolvio la pasarela y con comparacion exacta: las
     * pasarelas cambian su catalogo y una traduccion hecha hoy envejece. Se guarda
     * para poder revisar despues la traduccion; al cliente solo se le da su clase.
     */
    @Column(name = "gateway_decline_code", length = 50)
    private String gatewayDeclineCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "decline_kind", nullable = false, length = 15)
    private DeclineKind declineKind;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    /** Vacio en un rechazo duro: no hay siguiente hasta que haya otra tarjeta. */
    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PaymentAttemptJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public void setBillingDocumentId(Long billingDocumentId) {
        this.billingDocumentId = billingDocumentId;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Long paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public String getGatewayDeclineCode() {
        return gatewayDeclineCode;
    }

    public void setGatewayDeclineCode(String gatewayDeclineCode) {
        this.gatewayDeclineCode = gatewayDeclineCode;
    }

    public DeclineKind getDeclineKind() {
        return declineKind;
    }

    public void setDeclineKind(DeclineKind declineKind) {
        this.declineKind = declineKind;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
