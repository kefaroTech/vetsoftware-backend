package com.vetsoftware.app.publishadminpermissions.domain;

public record PublishAdminPermissionsResult(
    int companiesProcessed,
    int companiesUpdated,
    int permissionsCreated,
    int rolePermissionsCreated) {}
