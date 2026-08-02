package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "weight_records")
@SQLDelete(sql = "UPDATE weight_records SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class WeightRecordJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "animal_id", nullable = false)
  private AnimalJpaEntity animal;

  @Column(name = "weight_value", nullable = false, precision = 9, scale = 3)
  private BigDecimal value;

  @Enumerated(EnumType.STRING)
  @Column(name = "weight_unit", nullable = false, length = 20)
  private WeightType unit;

  @Column(name = "measured_at", nullable = false)
  private LocalDate measuredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 20)
  private WeightSource source;

  @Column(name = "source_id")
  private Long sourceId;

  @Column(name = "note", length = 500)
  private String note;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyJpaEntity company;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  protected WeightRecordJpaEntity() {}

  /**
   * Factory para escrituras cross-feature (consulta/hospitalización) que no pueden importar el
   * dominio de esta feature: reciben la unidad como String y resuelven el enum aquí, dentro del
   * paquete animal. Si {@code unit} es null se usa la unidad preferida del animal. Ver
   * [[permissions]]/vertical slicing.
   */
  public static WeightRecordJpaEntity of(
      AnimalJpaEntity animal,
      CompanyJpaEntity company,
      BigDecimal value,
      String unit,
      String source,
      Long sourceId,
      String note,
      LocalDate measuredAt) {
    if (value == null || value.signum() <= 0)
      throw new IllegalArgumentException("weight value must be greater than zero");
    WeightRecordJpaEntity e = new WeightRecordJpaEntity();
    e.animal = animal;
    e.company = company;
    e.value = value;
    e.unit = unit != null ? WeightType.valueOf(unit) : animal.getWeightType();
    e.source = WeightSource.valueOf(source);
    e.sourceId = sourceId;
    e.note = note;
    e.measuredAt = measuredAt != null ? measuredAt : LocalDate.now();
    e.createdDate = LocalDateTime.now();
    e.enabled = true;
    return e;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public AnimalJpaEntity getAnimal() {
    return animal;
  }

  public void setAnimal(AnimalJpaEntity animal) {
    this.animal = animal;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public WeightType getUnit() {
    return unit;
  }

  public void setUnit(WeightType unit) {
    this.unit = unit;
  }

  public LocalDate getMeasuredAt() {
    return measuredAt;
  }

  public void setMeasuredAt(LocalDate measuredAt) {
    this.measuredAt = measuredAt;
  }

  public WeightSource getSource() {
    return source;
  }

  public void setSource(WeightSource source) {
    this.source = source;
  }

  public Long getSourceId() {
    return sourceId;
  }

  public void setSourceId(Long sourceId) {
    this.sourceId = sourceId;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public CompanyJpaEntity getCompany() {
    return company;
  }

  public void setCompany(CompanyJpaEntity company) {
    this.company = company;
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
