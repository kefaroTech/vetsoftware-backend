package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Tabla {@code price_lists}.
 *
 * <p>
 * El {@code @SQLDelete} lleva {@code AND version = ?} porque la entidad esta
 * versionada: en cuanto hay {@code @Version}, Hibernate liga DOS parametros al
 * SQL -primero el id, despues la version- y un WHERE de un solo parametro se
 * rompe en ejecucion sin que el compilador diga nada
 * ({@code BORRADO_LOGICO_RESPETA_LA_VERSION}).
 *
 * <p>
 * <strong>{@code published_by_system_user_id} es una columna suelta, no una
 * asociacion.</strong> La FK a {@code system_users} existe en el esquema y no
 * hace falta modelarla en JPA: esta feature solo guarda quien firmo, nunca lee
 * sus datos. Colgar un {@code @ManyToOne} traeria un grafo ajeno a un slice que
 * a proposito no alcanza ninguna entidad con {@code company_id}, que es lo que
 * mantiene a las cuatro reglas duras de BE-COV fuera de esta feature.
 */
@Entity
@Table(name = "price_lists")
@SQLDelete(sql = "UPDATE price_lists SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class PriceListJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    // columnDefinition explicito: la columna es CHAR(3) y sin el Hibernate espera
    // varchar(3), con lo que `ddl-auto: validate` tumba el arranque de la
    // aplicacion entera con "found [char (Types#CHAR)], but expecting [varchar]".
    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PriceListStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by_system_user_id")
    private Long publishedBySystemUserId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected PriceListJpaEntity() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public PriceListStatus getStatus() {
        return status;
    }

    public void setStatus(PriceListStatus status) {
        this.status = status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getPublishedBySystemUserId() {
        return publishedBySystemUserId;
    }

    public void setPublishedBySystemUserId(Long publishedBySystemUserId) {
        this.publishedBySystemUserId = publishedBySystemUserId;
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
}
