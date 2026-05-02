package com.vetsoftware.app.state.infrastructure.web.response;

import java.time.LocalDateTime;

public record StateResponse(Long id, String name, CountrySummary country, LocalDateTime createdDate) {}
