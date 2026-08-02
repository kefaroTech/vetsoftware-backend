package com.vetsoftware.app.companysettings.application.dto;

import com.vetsoftware.app.companysettings.domain.CompanySetting;

public record CompanySettingDto(String propertyName, String value) {
  public static CompanySettingDto from(CompanySetting s) {
    return new CompanySettingDto(s.getPropertyName(), s.getValue());
  }
}
