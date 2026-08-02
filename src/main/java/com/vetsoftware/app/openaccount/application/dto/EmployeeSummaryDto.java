package com.vetsoftware.app.openaccount.application.dto;

import com.vetsoftware.app.openaccount.domain.EmployeeRef;

public record EmployeeSummaryDto(Long id, String name) {
  public static EmployeeSummaryDto from(EmployeeRef employee) {
    return new EmployeeSummaryDto(employee.id(), employee.name());
  }
}
