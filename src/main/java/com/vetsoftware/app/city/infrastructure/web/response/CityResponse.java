package com.vetsoftware.app.city.infrastructure.web.response;

import java.time.LocalDateTime;

public record CityResponse(Long id, String name, StateSummary state, LocalDateTime createdDate, boolean enabled) {}
