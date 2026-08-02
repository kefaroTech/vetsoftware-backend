package com.vetsoftware.app.systemuser.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "system_users")
@SQLDelete(
    sql = "UPDATE system_users SET enabled = false, auth_version = auth_version + 1 WHERE id = ?")
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

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "auth_version", nullable = false)
  private Long authVersion = 0L;

  protected SystemUserJpaEntity() {}

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
}
