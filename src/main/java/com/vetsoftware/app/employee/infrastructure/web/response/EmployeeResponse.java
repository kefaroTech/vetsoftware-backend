package com.vetsoftware.app.employee.infrastructure.web.response;

import java.time.LocalDateTime;

public record EmployeeResponse(Long id, String employeeCode, String name, String email,
                               String status, CompanySummary company,
                               LocalDateTime createdDate, Long createdBy) {}
