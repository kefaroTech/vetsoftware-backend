package com.vetsoftware.app.hospitalizationobservation.infrastructure.web.response;

import java.time.LocalDate;

public record HospitalizationSummary(Long id, LocalDate date) {}
