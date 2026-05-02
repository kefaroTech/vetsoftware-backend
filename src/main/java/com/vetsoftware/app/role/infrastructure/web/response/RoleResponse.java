package com.vetsoftware.app.role.infrastructure.web.response;

import java.time.LocalDateTime;

public record RoleResponse(Long id, String name, String code,
                           CompanySummary company,
                           LocalDateTime createdDate) {}
