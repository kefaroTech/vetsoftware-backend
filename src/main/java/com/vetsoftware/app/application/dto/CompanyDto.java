package com.vetsoftware.app.application.dto;

import com.vetsoftware.app.domain.Company;
import java.time.LocalDateTime;

public record CompanyDto(String id, String name, String identifier, String address,
                         String contactNumber, LocalDateTime createdDate, String createdBy) {
    public static CompanyDto from(Company company) {
        return new CompanyDto(
            company.getId().value(),
            company.getName(),
            company.getIdentifier(),
            company.getAddress(),
            company.getContactNumber(),
            company.getCreatedDate(),
            company.getCreatedBy()
        );
    }
}
