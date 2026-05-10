package com.vetsoftware.app.medicamentprescription.infrastructure.web.response;

import java.time.LocalDate;

public record PrescriptionSummary(Long id, LocalDate date) {}
