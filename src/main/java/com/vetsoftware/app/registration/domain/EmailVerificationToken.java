package com.vetsoftware.app.registration.domain;

import java.time.LocalDateTime;

/**
 * Token de un solo uso para verificar el correo del dueno tras el auto-registro. Persiste el HASH
 * del valor (no el valor plano). Las invariantes de consumo (no expirado, no reusado) viven aqui,
 * en el dominio.
 */
public class EmailVerificationToken {
  private final Long id;
  private final Long employeeId;
  private final Long companyId;
  private final String tokenHash;
  private final LocalDateTime expiresAt;
  private LocalDateTime consumedAt;

  public EmailVerificationToken(
      Long id,
      Long employeeId,
      Long companyId,
      String tokenHash,
      LocalDateTime expiresAt,
      LocalDateTime consumedAt) {
    if (employeeId == null) throw new IllegalArgumentException("employeeId is required");
    if (companyId == null) throw new IllegalArgumentException("companyId is required");
    if (tokenHash == null || tokenHash.isBlank())
      throw new IllegalArgumentException("tokenHash is required");
    if (expiresAt == null) throw new IllegalArgumentException("expiresAt is required");
    this.id = id;
    this.employeeId = employeeId;
    this.companyId = companyId;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.consumedAt = consumedAt;
  }

  public static EmailVerificationToken issue(
      Long employeeId, Long companyId, String tokenHash, LocalDateTime expiresAt) {
    return new EmailVerificationToken(null, employeeId, companyId, tokenHash, expiresAt, null);
  }

  /** Consume el token de forma irreversible. Falla si ya fue usado o si expiro. */
  public void consume(LocalDateTime now) {
    if (consumedAt != null) {
      throw new InvalidVerificationTokenException("Verification token already used");
    }
    if (now.isAfter(expiresAt)) {
      throw new InvalidVerificationTokenException("Verification token expired");
    }
    this.consumedAt = now;
  }

  public Long getId() {
    return id;
  }

  public Long getEmployeeId() {
    return employeeId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public LocalDateTime getConsumedAt() {
    return consumedAt;
  }
}
