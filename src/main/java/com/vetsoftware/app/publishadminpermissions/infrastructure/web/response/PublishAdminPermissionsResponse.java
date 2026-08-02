package com.vetsoftware.app.publishadminpermissions.infrastructure.web.response;

import com.vetsoftware.app.publishadminpermissions.application.dto.PublishAdminPermissionsDto;

public record PublishAdminPermissionsResponse(int companiesProcessed, int companiesUpdated,
        int permissionsCreated, int rolePermissionsCreated) {
    public static PublishAdminPermissionsResponse from(PublishAdminPermissionsDto dto) {
        return new PublishAdminPermissionsResponse(dto.companiesProcessed(), dto.companiesUpdated(),
                dto.permissionsCreated(), dto.rolePermissionsCreated());
    }
}
