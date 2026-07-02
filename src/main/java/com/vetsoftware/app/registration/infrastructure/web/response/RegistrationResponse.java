package com.vetsoftware.app.registration.infrastructure.web.response;

public record RegistrationResponse(Long companyId, Long employeeId, String token, String tokenType,
                                   String refreshToken) {}
