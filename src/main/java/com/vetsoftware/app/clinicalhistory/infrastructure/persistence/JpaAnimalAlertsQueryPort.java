package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.animalalert.infrastructure.persistence.AnimalAlertJpaRepository;
import com.vetsoftware.app.clinicalhistory.application.dto.ReportAlert;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalAlertsQueryPort;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee las alertas / alergias del animal para el banner de seguridad del reporte PDF. Cruce
 * cross-feature permitido: solo importa el {@code AnimalAlertJpaRepository} (persistencia).
 */
@Component
public class JpaAnimalAlertsQueryPort implements AnimalAlertsQueryPort {

  private final AnimalAlertJpaRepository animalAlertJpaRepository;

  public JpaAnimalAlertsQueryPort(AnimalAlertJpaRepository animalAlertJpaRepository) {
    this.animalAlertJpaRepository = animalAlertJpaRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReportAlert> findByAnimal(Long animalId, Long companyId) {
    return animalAlertJpaRepository
        .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(animalId, companyId)
        .stream()
        .map(
            e ->
                new ReportAlert(
                    typeLabel(e.getType() == null ? null : e.getType().name()),
                    e.getDescription(),
                    severityLabel(e.getSeverity() == null ? null : e.getSeverity().name())))
        .toList();
  }

  private static String typeLabel(String type) {
    if (type == null) return "";
    return switch (type) {
      case "ALLERGY" -> "Alergia";
      case "DRUG_REACTION" -> "Reacción a fármaco";
      case "CHRONIC_CONDITION" -> "Condición crónica";
      case "BEHAVIOR" -> "Comportamiento";
      case "OTHER" -> "Otra";
      default -> type;
    };
  }

  private static String severityLabel(String severity) {
    if (severity == null) return null;
    return switch (severity) {
      case "LOW" -> "Baja";
      case "MEDIUM" -> "Media";
      case "HIGH" -> "Alta";
      default -> severity;
    };
  }
}
