package com.vetsoftware.app.registration.application.port.out;

public interface RoleCreator {
    RoleResult create(String name, String code, Long companyId);

    record RoleResult(Long id) {
    }
}
