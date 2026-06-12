package com.vetsoftware.app.economicactivity.infrastructure.web.response;

import java.time.LocalDateTime;

public record EconomicActivityResponse(Long id, String code, String name, LocalDateTime createdDate, boolean enabled) {}
