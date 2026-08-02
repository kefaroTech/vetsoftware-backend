package com.vetsoftware.app.rolepermission.application.port.out;

public interface PermissionCachePort {
  void evictByRoleId(Long roleId);
}
