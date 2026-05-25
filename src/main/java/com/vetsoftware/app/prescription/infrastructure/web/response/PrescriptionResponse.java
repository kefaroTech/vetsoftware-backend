package com.vetsoftware.app.prescription.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionResponse(
        Long id,
        LocalDate date,
        String diagnosis,
        String observations,
        AnimalSummary animal,
        ConsultationSummary consultation,
        CompanySummary company,
        List<MedicamentSummary> medicaments,
        LocalDateTime createdDate,
        boolean enabled
) {}
