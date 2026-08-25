package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * Fila de {@code platform_access_requests}.
 *
 * <p>
 * <b>Lleva {@code @Version} de verdad, y no la exención de token.</b> La
 * tentación de exentarla como «token de un solo uso» es fuerte —tiene hash,
 * caducidad y consumo— pero esa exención dice «se emite, se consume y caduca;
 * nadie lo edita», y esta fila se edita hasta seis veces: cinco incrementos del
 * contador más la decisión. Y con concurrencia real: aprobar y rechazar pueden
 * llegar a la vez desde dos pestañas del mismo correo.
 *
 * <p>
 * <b>Ningún campo se llama {@code companyId} ni es de tipo
 * {@code CompanyJpaEntity}.</b> No es una omisión: la señal que enciende las
 * cuatro reglas duras de tenencia sobre <b>toda</b> la feature es un campo así
 * en cualquier entidad del paquete, seguido transitivamente. Este flujo es
 * global de plataforma y no tiene empresa. Renombrar un campo para esquivar la
 * señal está prohibido; aquí simplemente no hay nada que renombrar.
 *
 * <p>
 * <b>No hay {@code @PrePersist} con el reloj del sistema.</b>
 * {@code created_date} llega ya resuelto desde el caso de uso, que inyecta
 * {@code Clock}: un {@code LocalDateTime.now()} aquí sería una segunda fuente
 * de tiempo que ningún test puede fijar. {@code updatable = false} para que el
 * {@code UPDATE} de la decisión no intente reescribirlo.
 *
 * <p>
 * No hay {@code enabled} ni {@code @SQLDelete}: estas filas no son catálogo de
 * negocio sino credencial y bitácora, y se borran de verdad en la purga. Al no
 * haber un solo booleano, la trampa de {@code TINYINT(1)} no puede darse.
 */
@Entity
@Table(name = "platform_access_requests")
public class PlatformAccessRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "approval_token_hash", nullable = false, length = 64, unique = true)
    private String approvalTokenHash;

    /** bcrypt, no SHA-256: seis dígitos son 20 bits. Ver SecretHasherPort. */
    @Column(name = "verification_code_hash", nullable = false, length = 255)
    private String verificationCodeHash;

    @Column(name = "verification_attempts", nullable = false)
    private int verificationAttempts;

    /** Política congelada al emitir: un cambio del yml no reabre credenciales. */
    @Column(name = "max_attempts", nullable = false)
    private short maxAttempts;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** {@code APPROVED} o {@code REJECTED}; nunca un ENUM de MySQL. */
    @Column(name = "decision", length = 10)
    private String decision;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PlatformAccessRequestJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getApprovalTokenHash() {
        return approvalTokenHash;
    }

    public void setApprovalTokenHash(String approvalTokenHash) {
        this.approvalTokenHash = approvalTokenHash;
    }

    public String getVerificationCodeHash() {
        return verificationCodeHash;
    }

    public void setVerificationCodeHash(String verificationCodeHash) {
        this.verificationCodeHash = verificationCodeHash;
    }

    public int getVerificationAttempts() {
        return verificationAttempts;
    }

    public void setVerificationAttempts(int verificationAttempts) {
        this.verificationAttempts = verificationAttempts;
    }

    public short getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(short maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
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
