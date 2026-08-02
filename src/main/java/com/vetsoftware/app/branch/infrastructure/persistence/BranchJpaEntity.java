package com.vetsoftware.app.branch.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Sucursal (sede) de una empresa. {@code active} es un estado de negocio, NO un soft-delete: no
 * lleva {@code @SQLRestriction}/{@code @SQLDelete}, así una sucursal inactiva sigue siendo visible
 * (para reactivarla y para no romper el histórico que la referencia).
 */
@Entity
@Table(
    name = "branches",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_branches_company_code",
          columnNames = {"company_id", "code"})
    })
public class BranchJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 30)
  private String code;

  @Column(length = 255)
  private String address;

  @Column(length = 30)
  private String phone;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id", nullable = false)
  private CityJpaEntity city;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyJpaEntity company;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected BranchJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public CityJpaEntity getCity() {
    return city;
  }

  public void setCity(CityJpaEntity city) {
    this.city = city;
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

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
