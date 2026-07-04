package com.vetsoftware.app.prescription.application.port.out;

import com.vetsoftware.app.prescription.application.dto.PrescriptionReportModel;

/** Renderiza la fórmula médica veterinaria a PDF. */
public interface PrescriptionPdfPort {
    byte[] render(PrescriptionReportModel model);
}
