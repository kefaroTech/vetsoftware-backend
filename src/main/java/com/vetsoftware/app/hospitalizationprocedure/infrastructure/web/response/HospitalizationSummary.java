package com.vetsoftware.app.hospitalizationprocedure.infrastructure.web.response;

import java.time.LocalDate;

public record HospitalizationSummary(Long id, LocalDate date) {}
