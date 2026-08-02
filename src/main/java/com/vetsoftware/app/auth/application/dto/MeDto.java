package com.vetsoftware.app.auth.application.dto;

import java.util.Set;

public record MeDto(Long id, String type, Long companyId, String name, String employeeCode,
        boolean mustChangePassword, Set<String> permissions, Set<Long> branchIds) {
}
