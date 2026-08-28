package com.vetsoftware.app.limitdimension.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

/**
 * Fila del catálogo de ejes limitables.
 *
 * <p>
 * <strong>{@code sub_module_id} se mapea como columna, no como
 * asociación</strong>, y es deliberado: colgar un {@code ManyToOne} metería en
 * el grafo de esta feature todo lo que esa entidad alcance, y basta con que
 * alguna asociación llegue a la entidad de empresas para que las cuatro reglas
 * duras de BE-COV se activen sobre un slice que, por definición, no tiene
 * empresa a la que acotar. El {@code SubModuleRef} lo hidrata
 * {@code JpaSubModuleQueryPort} con una consulta aparte; el catálogo son ocho
 * filas y no hay N+1 posible.
 *
 * <p>
 * <strong>Sin borrado lógico anotado</strong>: la feature no expone borrado. Un
 * eje con contadores colgando no se puede retirar —las claves foráneas van
 * {@code RESTRICT}— y desactivarlo es lo que hace la columna {@code enabled}.
 */
@Entity
@Table(name = "limit_dimensions")
@SQLRestriction("enabled = true")
public class LimitDimensionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /**
     * El tipo de medida va como texto plano y no como enumerado JPA para que el
     * mapper sea el único punto que traduce: la columna es el destino de la clave
     * auxiliar {@code uq_limit_dimensions_id_measure_kind} y su valor lo copian
     * tres tablas más.
     */
    @Column(name = "measure_kind", nullable = false, length = 20)
    private String measureKind;

    @Column(name = "sub_module_id")
    private Long subModuleId;

    @Column(name = "release_delay_days")
    private Integer releaseDelayDays;

    @Column(name = "available_from", nullable = false)
    private LocalDate availableFrom;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected LimitDimensionJpaEntity() {
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

    public String getMeasureKind() {
        return measureKind;
    }

    public void setMeasureKind(String measureKind) {
        this.measureKind = measureKind;
    }

    public Long getSubModuleId() {
        return subModuleId;
    }

    public void setSubModuleId(Long subModuleId) {
        this.subModuleId = subModuleId;
    }

    public Integer getReleaseDelayDays() {
        return releaseDelayDays;
    }

    public void setReleaseDelayDays(Integer releaseDelayDays) {
        this.releaseDelayDays = releaseDelayDays;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDate availableFrom) {
        this.availableFrom = availableFrom;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
