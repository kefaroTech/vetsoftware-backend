package com.vetsoftware.app.prescription.application.port.out;

import com.vetsoftware.app.prescription.application.dto.PrescriptionSignalment;
import java.util.Optional;

/**
 * Carga los datos de clínica, paciente y propietario para la fórmula, scoped a
 * la empresa.
 */
public interface PrescriptionReportQueryPort {
    Optional<PrescriptionSignalment> loadByAnimal(Long animalId, Long companyId);
}
