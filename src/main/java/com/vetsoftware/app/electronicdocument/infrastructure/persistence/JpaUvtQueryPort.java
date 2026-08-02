package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.UvtQueryPort;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaEntity;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter del {@link UvtQueryPort}: lee la fila 'uvt' de system_configurations (otra feature) y
 * parsea su valor de texto a número. Único cruce permitido de vertical slicing (persistence →
 * persistence de otra feature).
 */
@Component
public class JpaUvtQueryPort implements UvtQueryPort {
  private static final String UVT_PROPERTY = "uvt";

  private final SystemConfigurationJpaRepository systemConfigurationJpaRepository;

  public JpaUvtQueryPort(SystemConfigurationJpaRepository systemConfigurationJpaRepository) {
    this.systemConfigurationJpaRepository = systemConfigurationJpaRepository;
  }

  @Override
  public Optional<BigDecimal> currentUvt() {
    return systemConfigurationJpaRepository
        .findByPropertyName(UVT_PROPERTY)
        .map(SystemConfigurationJpaEntity::getValue)
        .flatMap(JpaUvtQueryPort::parse);
  }

  private static Optional<BigDecimal> parse(String value) {
    if (value == null || value.isBlank()) return Optional.empty();
    try {
      return Optional.of(new BigDecimal(value.trim()));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
