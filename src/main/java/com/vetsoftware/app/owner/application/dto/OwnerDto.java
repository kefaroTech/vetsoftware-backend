package com.vetsoftware.app.owner.application.dto;

import com.vetsoftware.app.owner.domain.Owner;
import java.time.LocalDateTime;

public record OwnerDto(
        Long id, String name, String email, String document, String address,
        String phone, CitySummaryDto city, CompanySummaryDto company, LocalDateTime createdDate
) {
    public static OwnerDto from(Owner owner) {
        return new OwnerDto(
            owner.getId(), owner.getName(), owner.getEmail(), owner.getDocument(),
            owner.getAddress(), owner.getPhone(),
            CitySummaryDto.from(owner.getCity()),
            CompanySummaryDto.from(owner.getCompany()),
            owner.getCreatedDate()
        );
    }
}
