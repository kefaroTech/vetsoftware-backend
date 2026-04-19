package com.vetsoftware.app.application.dto;

import com.vetsoftware.app.domain.Company;
import java.time.LocalDateTime;

public record CompanyDto(Long id, String name, String identifier, String address,
                         String contactNumber, LocalDateTime createdDate, Long createdBy) {
    public static CompanyDto from(Company company) {
        return new CompanyDto(
            company.getId(),
            company.getName(),
            company.getIdentifier(),
            company.getAddress(),
            company.getContactNumber(),
            company.getCreatedDate(),
            company.getCreatedBy()
        );
    }
}
