package com.vetsoftware.app.branch.application.dto;

import com.vetsoftware.app.branch.domain.Branch;
import java.time.LocalDateTime;

public record BranchDto(
        Long id, String name, String code, String address, String phone,
        CitySummaryDto city, CompanySummaryDto company, LocalDateTime createdDate, boolean active
) {
    public static BranchDto from(Branch branch) {
        return new BranchDto(
            branch.getId(), branch.getName(), branch.getCode(), branch.getAddress(), branch.getPhone(),
            CitySummaryDto.from(branch.getCity()),
            CompanySummaryDto.from(branch.getCompany()),
            branch.getCreatedDate(), branch.isActive()
        );
    }
}
