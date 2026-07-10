package com.vetsoftware.app.employee.infrastructure.web.response;

import java.time.LocalDateTime;
import java.util.List;

public record EmployeeResponse(Long id, String employeeCode, String name, String email,
                               CompanySummary company,
                               List<RoleSummary> roles,
                               LocalDateTime createdDate,
                               boolean enabled,
                               boolean mustChangePassword,
                               String status) {}
