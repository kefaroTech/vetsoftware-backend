package com.vetsoftware.app.systemconfiguration.application.dto;

import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import java.time.LocalDateTime;

public record SystemConfigurationDto(
    Long id, String propertyName, String value, LocalDateTime createdDate, boolean enabled) {

  public static SystemConfigurationDto from(SystemConfiguration config) {
    return new SystemConfigurationDto(
        config.getId(),
        config.getPropertyName(),
        config.getValue(),
        config.getCreatedDate(),
        config.isEnabled());
  }
}
