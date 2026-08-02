package com.vetsoftware.app.animalcolor.infrastructure.web.response;

import java.time.LocalDateTime;

public record AnimalColorResponse(Long id, String name, LocalDateTime createdDate,
        boolean enabled) {
}
