package com.vetsoftware.app.registration.infrastructure.web.response;

public record RegistrationResponse(Long companyId, Long employeeId, String email, String employeeCode,
                                   String status) {}
