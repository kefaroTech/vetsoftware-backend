package com.vetsoftware.app.owner.infrastructure.web.response;

import java.time.LocalDateTime;

public record OwnerResponse(
        Long id, String name, String email, String document, String address,
        String phone, CitySummary city, CompanySummary company, LocalDateTime createdDate
) {}
