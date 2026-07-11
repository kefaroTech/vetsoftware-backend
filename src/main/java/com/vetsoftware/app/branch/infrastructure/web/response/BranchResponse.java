package com.vetsoftware.app.branch.infrastructure.web.response;

import java.time.LocalDateTime;

public record BranchResponse(
        Long id, String name, String code, String address, String phone,
        CitySummary city, CompanySummary company, LocalDateTime createdDate, boolean active
) {}
