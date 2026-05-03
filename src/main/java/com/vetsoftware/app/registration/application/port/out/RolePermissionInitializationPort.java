package com.vetsoftware.app.registration.application.port.out;

public interface RolePermissionInitializationPort {
    void initializeForRole(Long roleId, Long companyId, Long baseRoleId, Long membershipId);
}
