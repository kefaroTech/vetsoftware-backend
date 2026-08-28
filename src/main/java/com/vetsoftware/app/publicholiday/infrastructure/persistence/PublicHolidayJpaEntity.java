package com.vetsoftware.app.publicholiday.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Espejo de {@code public_holidays} (changeset 350).
 *
 * <p>
 * <strong>Sin {@code @Version}, exenta {@code E1_APPEND_ONLY}</strong> y con su
 * linea escrita en {@code ENTIDADES_EXENTAS_DE_VERSION}: un festivo se siembra
 * por ano y nadie lo reescribe, asi que no hay ciclo leer-modificar-guardar que
 * el bloqueo optimista pudiera proteger. Tampoco lleva {@code @SQLDelete}: no
 * se borra en logico desde ningun caso de uso.
 */
@Entity
@Table(name = "public_holidays")
public class PublicHolidayJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "nominal_date")
    private LocalDate nominalDate;

    @Column(name = "moved", nullable = false)
    private boolean moved;

    @Column(name = "legal_reference", nullable = false, length = 255)
    private String legalReference;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected PublicHolidayJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getNominalDate() {
        return nominalDate;
    }

    public void setNominalDate(LocalDate nominalDate) {
        this.nominalDate = nominalDate;
    }

    public boolean isMoved() {
        return moved;
    }

    public void setMoved(boolean moved) {
        this.moved = moved;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public void setLegalReference(String legalReference) {
        this.legalReference = legalReference;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
