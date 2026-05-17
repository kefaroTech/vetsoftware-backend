package com.vetsoftware.app.employeerole.application.port.out;

public interface PermissionCachePort {
    void evictByEmployeeId(Long employeeId);
}
