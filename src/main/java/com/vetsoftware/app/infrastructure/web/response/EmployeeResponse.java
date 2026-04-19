package com.vetsoftware.app.infrastructure.web.response;

import java.time.LocalDateTime;

public record EmployeeResponse(Long id, String employeeCode, String name, String email,
                               String status, Long companyId, LocalDateTime createdDate, Long createdBy) {}
