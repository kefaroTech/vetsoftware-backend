package com.vetsoftware.app.module.infrastructure.web.response;

import java.time.LocalDateTime;

public record ModuleResponse(Long id, String name, String code, LocalDateTime createdDate,
        boolean enabled) {
}
