package com.vetsoftware.app.economicactivity.application.dto;

import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import java.time.LocalDateTime;

public record EconomicActivityDto(
    Long id, String code, String name, LocalDateTime createdDate, boolean enabled) {
  public static EconomicActivityDto from(EconomicActivity economicActivity) {
    return new EconomicActivityDto(
        economicActivity.getId(),
        economicActivity.getCode(),
        economicActivity.getName(),
        economicActivity.getCreatedDate(),
        economicActivity.isEnabled());
  }
}
