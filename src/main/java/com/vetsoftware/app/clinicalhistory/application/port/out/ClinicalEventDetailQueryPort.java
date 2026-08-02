package com.vetsoftware.app.clinicalhistory.application.port.out;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDetail;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.util.Optional;

/**
 * Carga el detalle clínico rico de un evento desde su entidad fuente (consulta,
 * cirugía, vacunación, …) para el reporte PDF. Devuelve el título y los campos
 * ya ordenados y sin valores vacíos; {@code empty} si el evento no existe o
 * pertenece a otra empresa.
 */
public interface ClinicalEventDetailQueryPort {

    Optional<ClinicalEventDetail> load(ClinicalEventType type, Long sourceId, Long companyId);
}
