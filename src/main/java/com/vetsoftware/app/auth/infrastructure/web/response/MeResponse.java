package com.vetsoftware.app.auth.infrastructure.web.response;

import java.util.List;

public record MeResponse(
    Long id,
    String type,
    Long companyId,
    String name,
    String employeeCode,
    boolean mustChangePassword,
    List<String> permissions,
    List<Long> branchIds) {}
