package com.vetsoftware.app.prescription.application.dto;

import com.vetsoftware.app.prescription.domain.MedicamentRef;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Modelo de la fórmula médica veterinaria para el PDF (Thymeleaf + Gotenberg). */
public record PrescriptionReportModel(
        PrescriptionSignalment signalment,
        String prescriberName,
        LocalDate date,
        String diagnosis,
        String observations,
        List<MedicamentRef> medicaments,
        LocalDateTime generatedAt
) {
}
