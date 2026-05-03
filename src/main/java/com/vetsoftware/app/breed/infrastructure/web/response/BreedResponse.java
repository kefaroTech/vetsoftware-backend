package com.vetsoftware.app.breed.infrastructure.web.response;

import java.time.LocalDateTime;

public record BreedResponse(Long id, String name, SpecieSummary specie, LocalDateTime createdDate) {}
