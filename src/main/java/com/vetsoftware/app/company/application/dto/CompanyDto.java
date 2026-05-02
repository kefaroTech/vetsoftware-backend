package com.vetsoftware.app.company.application.dto;

import com.vetsoftware.app.company.domain.Company;
import java.time.LocalDateTime;

public record CompanyDto(Long id, String name, String identifier, String address,
                         String contactNumber, CitySummaryDto city, LocalDateTime createdDate) {
    public static CompanyDto from(Company company) {
        return new CompanyDto(
            company.getId(),
            company.getName(),
            company.getIdentifier(),
            company.getAddress(),
            company.getContactNumber(),
            CitySummaryDto.from(company.getCity()),
            company.getCreatedDate()
        );
    }
}
