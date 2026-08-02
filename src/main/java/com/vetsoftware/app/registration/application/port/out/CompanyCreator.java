package com.vetsoftware.app.registration.application.port.out;

public interface CompanyCreator {
    CompanyResult create(String name, String identifier, String address, String contactNumber,
            Long cityId, Long membershipId);

    record CompanyResult(Long id, String name, String identifier) {
    }
}
