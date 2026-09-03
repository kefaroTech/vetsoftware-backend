package com.vetsoftware.app.systemuser.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "system_users")
@SQLDelete(sql = "UPDATE system_users SET enabled = false, auth_version = auth_version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class SystemUserJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "hash_password", nullable = false, length = 255)
    private String hashPassword;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "auth_version", nullable = false)
    private Long authVersion = 0L;

    /**
     * Correo de la cuenta. Nullable: las filas heredadas no lo tienen y no se les
     * puede inventar uno. El UNIQUE de la base convive con todos esos NULL —MySQL
     * admite multiples nulos en un indice unico— y esa propiedad del motor es lo
     * que permite desplegar la columna sin backfill y sin parar nada.
     *
     * <p>
     * VARCHAR(150) y no 100 como employees.email: el formulario publico acepta 150
     * y la solicitud los guarda, asi que con 100 aqui un correo de 101 a 150
     * caracteres pasaria la solicitud, pasaria la aprobacion, llegaria a la
     * pantalla de crear contrasena y reventaria en el INSERT final, que es el unico
     * momento del flujo en el que ya no hay reintento posible.
     */
    @Column(name = "email", length = 150, unique = true)
    private String email;

    @Column(name = "full_name", length = 120)
    private String fullName;

    protected SystemUserJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getHashPassword() {
        return hashPassword;
    }

    public void setHashPassword(String hashPassword) {
        this.hashPassword = hashPassword;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getAuthVersion() {
        return authVersion;
    }

    public void setAuthVersion(Long authVersion) {
        this.authVersion = authVersion;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
