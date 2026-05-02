package com.vetsoftware.app.country.infrastructure.web.response;

import java.time.LocalDateTime;

public record CountryResponse(Long id, String name, LocalDateTime createdDate) {}
