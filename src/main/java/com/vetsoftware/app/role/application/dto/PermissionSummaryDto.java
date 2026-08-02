package com.vetsoftware.app.role.application.dto;

public record PermissionSummaryDto(Long rolePermissionId, Long id, String name, String code) {
  public PermissionSummaryDto {
    if (rolePermissionId == null)
      throw new IllegalArgumentException("rolePermissionId is required");
    if (id == null) throw new IllegalArgumentException("permission id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("permission name is required");
    if (code == null || code.isBlank())
      throw new IllegalArgumentException("permission code is required");
  }
}
