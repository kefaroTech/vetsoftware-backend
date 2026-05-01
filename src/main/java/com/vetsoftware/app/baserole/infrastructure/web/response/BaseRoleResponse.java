package com.vetsoftware.app.baserole.infrastructure.web.response;

import java.time.LocalDateTime;

public record BaseRoleResponse(Long id, String name, String code, Boolean mandatory, LocalDateTime createdDate) {}
