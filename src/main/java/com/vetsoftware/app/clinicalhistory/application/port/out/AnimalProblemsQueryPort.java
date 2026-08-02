package com.vetsoftware.app.clinicalhistory.application.port.out;

import com.vetsoftware.app.clinicalhistory.application.dto.ReportProblem;
import java.util.List;

/** Lista de problemas (POMR) del animal para el reporte PDF; ya ordenada (activos primero). */
public interface AnimalProblemsQueryPort {
  List<ReportProblem> findByAnimal(Long animalId, Long companyId);
}
