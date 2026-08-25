package com.vetsoftware.app.systemuser.domain;

import java.time.LocalDateTime;

public class SystemUser {

    public static final int EMAIL_MAX = 150;
    public static final int FULL_NAME_MAX = 120;

    private Long id;
    private String code;
    private String hashPassword;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;
    private Long authVersion;
    /**
     * Correo de la cuenta. <b>Nullable, y lo seguirá siendo.</b> Las filas
     * heredadas no tienen correo y no se les puede inventar uno; el paso de ponerlo
     * obligatorio no está planificado. El {@code UNIQUE} de la columna convive con
     * todos esos {@code NULL} porque MySQL admite múltiples nulos en un índice
     * único, que es lo que permite desplegar la columna sin backfill.
     *
     * <p>
     * <b>No sustituye a {@code code}</b>, que sigue siendo el identificador de
     * login. Meter el correo ahí sería el antipatrón de la columna con dos
     * significados y rompería el login de los superadministradores actuales.
     */
    private String email;
    /**
     * Nombre real de la persona. Antes se perdía: {@code /auth/me} devolvía el
     * código.
     */
    private String fullName;

    public SystemUser(Long id, String code, String hashPassword, LocalDateTime createdDate,
            Long version, boolean enabled, Long authVersion) {
        this(id, code, hashPassword, createdDate, version, enabled, authVersion, null, null);
    }

    public SystemUser(Long id, String code, String hashPassword, LocalDateTime createdDate,
            Long version, boolean enabled, Long authVersion, String email, String fullName) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 50)
            throw new IllegalArgumentException("code must be 50 chars or less");
        if (hashPassword == null || hashPassword.isBlank())
            throw new IllegalArgumentException("password is required");
        if (email != null && email.length() > EMAIL_MAX)
            throw new IllegalArgumentException("email must be " + EMAIL_MAX + " chars or less");
        if (fullName != null && fullName.length() > FULL_NAME_MAX)
            throw new IllegalArgumentException(
                    "fullName must be " + FULL_NAME_MAX + " chars or less");
        this.id = id;
        this.code = code;
        this.hashPassword = hashPassword;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
        this.authVersion = authVersion == null ? 0L : authVersion;
        this.email = email;
        this.fullName = fullName;
    }

    public static SystemUser create(String code, String hashPassword) {
        return new SystemUser(null, code, hashPassword, LocalDateTime.now(), null, true, 0L);
    }

    /**
     * Alta con identidad completa, para la cuenta que nace de una invitación de
     * plataforma. Recibe {@code createdDate} en vez de leer el reloj del sistema:
     * el instante lo decide el caso de uso, que inyecta {@code Clock} y por tanto
     * se puede fijar en un test.
     */
    public static SystemUser provision(String code, String hashPassword, String email,
            String fullName, LocalDateTime createdDate) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("email is required");
        if (fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("fullName is required");
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
        return new SystemUser(null, code, hashPassword, createdDate, null, true, 0L, email,
                fullName);
    }

    public void update(String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 50)
            throw new IllegalArgumentException("code must be 50 chars or less");
        this.code = code;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getAuthVersion() {
        return authVersion;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }
}
