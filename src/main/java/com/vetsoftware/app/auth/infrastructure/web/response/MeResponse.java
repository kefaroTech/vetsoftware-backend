package com.vetsoftware.app.auth.infrastructure.web.response;

import java.util.List;

public record MeResponse(
        Long id,
        String type,
        Long companyId,
        String name,
        String employeeCode,
        List<String> permissions
) {}
