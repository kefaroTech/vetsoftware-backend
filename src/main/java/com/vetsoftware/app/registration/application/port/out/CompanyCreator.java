package com.vetsoftware.app.registration.application.port.out;

public interface CompanyCreator {
    CompanyResult create(String name, String identifier, String address, String contactNumber);

    record CompanyResult(Long id, String name, String identifier) {}
}
