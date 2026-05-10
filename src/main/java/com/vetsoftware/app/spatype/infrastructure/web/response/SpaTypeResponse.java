package com.vetsoftware.app.spatype.infrastructure.web.response;

import java.time.LocalDateTime;

public record SpaTypeResponse(Long id, String name, String description, LocalDateTime createdDate) {}
