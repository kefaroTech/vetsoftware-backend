package com.vetsoftware.app.publishadminpermissions.application.dto;

import com.vetsoftware.app.publishadminpermissions.domain.PublishAdminPermissionsResult;

public record PublishAdminPermissionsDto(
    int companiesProcessed,
    int companiesUpdated,
    int permissionsCreated,
    int rolePermissionsCreated
) {
    public static PublishAdminPermissionsDto from(PublishAdminPermissionsResult result) {
        return new PublishAdminPermissionsDto(
            result.companiesProcessed(),
            result.companiesUpdated(),
            result.permissionsCreated(),
            result.rolePermissionsCreated()
        );
    }
}
