package com.vetsoftware.app.platformaccess.domain;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Solicitud pública de alta de un superadministrador de plataforma. Guarda el
 * HASH del token del aprobador (SHA-256) y el HASH del código de 6 dígitos
 * (bcrypt); ninguno de los dos valores planos se persiste.
 *
 * <p>
 * <b>No hay columna {@code status}, y es deliberado.</b> El estado se deriva
 * íntegramente de la fila y del instante de lectura, con esta precedencia, que
 * es obligatoria:
 *
 * <pre>
 * BLOCKED  &lt;= verificationAttempts &gt;= maxAttempts   (persistente, gana a todo)
 * APPROVED &lt;= decision = APPROVED
 * REJECTED &lt;= decision = REJECTED
 * EXPIRED  &lt;= decision es null y now &gt; expiresAt
 * PENDING  &lt;= el resto
 * </pre>
 *
 * Una quinta fuente de verdad junto a {@code decision}, {@code decidedAt},
 * {@code expiresAt} y {@code verificationAttempts} podría desincronizarse de
 * las otras cuatro y ninguna constraint la mantendría en línea con el reloj.
 *
 * <p>
 * <b>{@code maxAttempts} vive en la fila, no en el {@code application.yml}.</b>
 * El límite con el que se emitió una credencial es una propiedad de esa
 * credencial, no del despliegue actual: bajar la política de 5 a 3 mañana
 * desbloquearía o bloquearía retroactivamente credenciales ya emitidas, que es
 * un control de seguridad que muta hacia atrás.
 *
 * <p>
 * {@code fullName} y {@code reason} son texto libre de un desconocido que acaba
 * pintado en el buzón de quien puede crear superadministradores. Sus topes se
 * validan aquí además de en el {@code request} REST, y <b>los dos</b> rechazan
 * los caracteres de control: un {@code fullName} con retorno de carro es
 * inyección de cabeceras si alguna vez alguien lo mete en el asunto del correo,
 * y el motivo viaja al mismo buzón por el mismo camino. La única excepción es
 * el salto de línea dentro de {@code reason}, que es un campo de texto largo;
 * el porqué, y por qué el retorno de carro no entra en esa excepción, está en
 * {@code requireWithoutControlCharacters}.
 */
public class PlatformAccessRequest {

    public static final int FULL_NAME_MIN = 3;
    public static final int FULL_NAME_MAX = 120;
    public static final int EMAIL_MAX = 150;
    public static final int REASON_MIN = 20;
    public static final int REASON_MAX = 500;
    public static final int TOKEN_HASH_LENGTH = 64;

    private final Long id;
    private final String fullName;
    private final String email;
    private final String reason;
    private final String approvalTokenHash;
    private final String verificationCodeHash;
    private final int verificationAttempts;
    private final int maxAttempts;
    private final LocalDateTime expiresAt;
    private final PlatformAccessDecision decision;
    private final LocalDateTime decidedAt;
    private final LocalDateTime createdDate;
    private final Long version;

    public PlatformAccessRequest(Long id, String fullName, String email, String reason,
            String approvalTokenHash, String verificationCodeHash, int verificationAttempts,
            int maxAttempts, LocalDateTime expiresAt, PlatformAccessDecision decision,
            LocalDateTime decidedAt, LocalDateTime createdDate, Long version) {
        requireText(fullName, "fullName", FULL_NAME_MIN, FULL_NAME_MAX);
        requireWithoutControlCharacters(fullName, "fullName");
        requireText(email, "email", 1, EMAIL_MAX);
        requireWithoutControlCharacters(email, "email");
        requireText(reason, "reason", REASON_MIN, REASON_MAX);
        requireWithoutControlCharacters(reason, "reason", true);
        if (approvalTokenHash == null || approvalTokenHash.length() != TOKEN_HASH_LENGTH) {
            throw new IllegalArgumentException("approvalTokenHash must be a 64 char hex digest");
        }
        if (verificationCodeHash == null || verificationCodeHash.isBlank()) {
            throw new IllegalArgumentException("verificationCodeHash is required");
        }
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException("maxAttempts must be between 1 and 10");
        }
        if (verificationAttempts < 0 || verificationAttempts > maxAttempts) {
            throw new IllegalArgumentException("verificationAttempts is out of range");
        }
        if (createdDate == null) {
            throw new IllegalArgumentException("createdDate is required");
        }
        if (expiresAt == null || !expiresAt.isAfter(createdDate)) {
            throw new IllegalArgumentException("expiresAt must be after createdDate");
        }
        if ((decision == null) != (decidedAt == null)) {
            throw new IllegalArgumentException("decision and decidedAt must be set together");
        }
        if (decidedAt != null && decidedAt.isBefore(createdDate)) {
            throw new IllegalArgumentException("decidedAt cannot precede createdDate");
        }
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.reason = reason;
        this.approvalTokenHash = approvalTokenHash;
        this.verificationCodeHash = verificationCodeHash;
        this.verificationAttempts = verificationAttempts;
        this.maxAttempts = maxAttempts;
        this.expiresAt = expiresAt;
        this.decision = decision;
        this.decidedAt = decidedAt;
        this.createdDate = createdDate;
        this.version = version;
    }

    /** Solicitud recién recibida: sin decisión, sin intentos y sin id. */
    public static PlatformAccessRequest issue(String fullName, String email, String reason,
            String approvalTokenHash, String verificationCodeHash, int maxAttempts,
            LocalDateTime createdDate, LocalDateTime expiresAt) {
        return new PlatformAccessRequest(null, fullName, email, reason, approvalTokenHash,
                verificationCodeHash, 0, maxAttempts, expiresAt, null, null, createdDate, null);
    }

    /** Terminal y permanente: ninguna espera lo revierte. Gana a todo lo demás. */
    public boolean isBlocked() {
        return verificationAttempts >= maxAttempts;
    }

    public boolean isDecided() {
        return decision != null;
    }

    public boolean isExpired(LocalDateTime now) {
        return decision == null && now.isAfter(expiresAt);
    }

    /** {@code true} si todavía admite una decisión en este instante. */
    public boolean isPending(LocalDateTime now) {
        return !isBlocked() && !isDecided() && !isExpired(now);
    }

    public int remainingAttempts() {
        return Math.max(0, maxAttempts - verificationAttempts);
    }

    /**
     * Segundos transcurridos desde que la solicitud se decidió. Es lo que separa el
     * doble clic del aprobador (segundos) de la reproducción de un correo filtrado
     * (horas o días) al leer el evento {@code token_consumed}.
     */
    public long secondsSinceDecision(LocalDateTime now) {
        if (decidedAt == null) {
            return 0L;
        }
        return Duration.between(decidedAt, now).toSeconds();
    }

    /**
     * Dominio del correo del solicitante, y solo el dominio. Es lo único de esta
     * dirección que puede salir al log: responde si cuarenta solicitudes vienen de
     * cuarenta dominios desechables o de tres personas de la misma empresa, sin
     * identificar a nadie.
     */
    public String emailDomain() {
        int at = email.lastIndexOf("@");
        return at < 0 || at == email.length() - 1 ? "" : email.substring(at + 1);
    }

    private static void requireText(String value, String field, int min, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.length() < min) {
            throw new IllegalArgumentException(field + " must be at least " + min + " chars");
        }
        if (value.length() > max) {
            throw new IllegalArgumentException(field + " must be " + max + " chars or less");
        }
    }

    private static void requireWithoutControlCharacters(String value, String field) {
        requireWithoutControlCharacters(value, field, false);
    }

    /**
     * Rechaza los caracteres de control. {@code allowLineFeed} exceptúa el salto de
     * línea y solo el salto de línea.
     *
     * <p>
     * <b>La decisión, escrita, porque {@code reason} y {@code fullName} no podían
     * seguir divergiendo.</b> {@code fullName} los rechazaba todos y {@code reason}
     * ninguno, y los dos acaban pintados en el buzón de quien puede crear
     * superadministradores. La asimetría no respondía a ningún criterio: era el
     * hueco de que la comprobación se escribió pensando en el nombre.
     *
     * <p>
     * No se resuelve igualando por arriba. Un motivo de 20 a 500 caracteres es un
     * campo de texto largo y el salto de línea es el único carácter de control que
     * una persona teclea ahí a propósito; prohibirlo convertiría un párrafo normal
     * en un 400 que el solicitante no sabría corregir. Un nombre completo, en
     * cambio, no tiene ninguna razón para llevar uno.
     *
     * <p>
     * <b>El retorno de carro queda fuera de la excepción, y esa es la mitad que
     * importa.</b> {@code CR} es la mitad de {@code CRLF}, que es la forma de la
     * inyección de cabeceras; un {@code textarea} nunca lo produce en un cuerpo
     * JSON —la API del elemento normaliza sus saltos a {@code LF}—, así que
     * admitirlo no daría nada a un cliente legítimo y sí a uno que no lo es.
     */
    private static void requireWithoutControlCharacters(String value, String field,
            boolean allowLineFeed) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (allowLineFeed && character == '\n') {
                continue;
            }
            if (Character.isISOControl(character)) {
                throw new IllegalArgumentException(field + " cannot contain control characters");
            }
        }
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getReason() {
        return reason;
    }

    public String getApprovalTokenHash() {
        return approvalTokenHash;
    }

    public String getVerificationCodeHash() {
        return verificationCodeHash;
    }

    public int getVerificationAttempts() {
        return verificationAttempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public PlatformAccessDecision getDecision() {
        return decision;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
