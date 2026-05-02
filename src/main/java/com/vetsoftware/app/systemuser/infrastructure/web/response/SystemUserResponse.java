package com.vetsoftware.app.systemuser.infrastructure.web.response;

import java.time.LocalDateTime;

public record SystemUserResponse(Long id, String code, LocalDateTime createdDate) {}
