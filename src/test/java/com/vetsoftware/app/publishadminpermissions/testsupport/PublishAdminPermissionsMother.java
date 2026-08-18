package com.vetsoftware.app.publishadminpermissions.testsupport;

import com.vetsoftware.app.publishadminpermissions.application.port.out.AdminBasePermission;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyAdminContext;
import com.vetsoftware.app.publishadminpermissions.application.port.out.UpsertedPermission;

public final class PublishAdminPermissionsMother {

    private PublishAdminPermissionsMother() {
    }

    public static AdminBasePermission verAnimales() {
        return new AdminBasePermission(101L, "animal.read", "Ver animales", 5L);
    }

    public static AdminBasePermission verFacturas() {
        return new AdminBasePermission(102L, "invoice.read", "Ver facturas", 6L);
    }

    public static CompanyAdminContext clinicaNorte() {
        return new CompanyAdminContext(1L, 10L, 100L);
    }

    public static CompanyAdminContext clinicaSur() {
        return new CompanyAdminContext(2L, 20L, 200L);
    }

    public static UpsertedPermission creado(Long id) {
        return new UpsertedPermission(id, true);
    }

    public static UpsertedPermission existente(Long id) {
        return new UpsertedPermission(id, false);
    }
}
