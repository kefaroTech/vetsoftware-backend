package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web.response;

import java.time.LocalDate;

public record HospitalizationSummary(Long id, LocalDate date) {}
