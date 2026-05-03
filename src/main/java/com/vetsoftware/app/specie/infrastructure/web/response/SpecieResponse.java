package com.vetsoftware.app.specie.infrastructure.web.response;

import java.time.LocalDateTime;

public record SpecieResponse(Long id, String name, LocalDateTime createdDate) {}
