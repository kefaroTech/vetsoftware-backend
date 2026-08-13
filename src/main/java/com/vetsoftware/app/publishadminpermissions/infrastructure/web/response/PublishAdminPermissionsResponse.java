package com.vetsoftware.app.publishadminpermissions.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.publishadminpermissions.application.dto.PublishAdminPermissionsDto;

public record PublishAdminPermissionsResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int companiesProcessed,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int companiesUpdated,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int permissionsCreated,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int rolePermissionsCreated) {
    public static PublishAdminPermissionsResponse from(PublishAdminPermissionsDto dto) {
        return new PublishAdminPermissionsResponse(dto.companiesProcessed(), dto.companiesUpdated(),
                dto.permissionsCreated(), dto.rolePermissionsCreated());
    }
}
