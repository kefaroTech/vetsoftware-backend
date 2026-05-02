package com.vetsoftware.app.permission.infrastructure.web.response;

import java.time.LocalDateTime;

public record PermissionResponse(Long id, String name, String code,
                                  CompanySummary company, SubModuleSummary subModule,
                                  LocalDateTime createdDate) {}
