package com.vetsoftware.app.clinicalhistory.application.port.out;

import com.vetsoftware.app.clinicalhistory.application.dto.ReportAlert;
import java.util.List;

/** Alertas / alergias del animal para el banner de seguridad del reporte PDF. */
public interface AnimalAlertsQueryPort {
    List<ReportAlert> findByAnimal(Long animalId, Long companyId);
}
