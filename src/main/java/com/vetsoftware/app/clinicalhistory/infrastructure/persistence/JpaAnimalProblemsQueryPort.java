package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.clinicalhistory.application.dto.ReportProblem;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalProblemsQueryPort;
import com.vetsoftware.app.problem.infrastructure.persistence.ProblemJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee la lista de problemas (POMR) del animal para el reporte PDF. Cruce
 * cross-feature permitido: solo importa el {@code ProblemJpaRepository}
 * (persistencia), nunca su dominio (los enums se resuelven vía
 * {@code .name()}).
 */
@Component
public class JpaAnimalProblemsQueryPort implements AnimalProblemsQueryPort {

    private final ProblemJpaRepository problemJpaRepository;

    public JpaAnimalProblemsQueryPort(ProblemJpaRepository problemJpaRepository) {
        this.problemJpaRepository = problemJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportProblem> findByAnimal(Long animalId, Long companyId) {
        return problemJpaRepository
                .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(animalId, companyId).stream()
                .map(e -> {
                    String status = e.getStatus() == null ? null : e.getStatus().name();
                    return new ReportProblem(e.getDescription(), statusLabel(status),
                            !"RESOLVED".equals(status), e.getOnsetDate(), e.getResolvedDate(),
                            e.getNotes());
                }).toList();
    }

    private static String statusLabel(String status) {
        if (status == null)
            return "";
        return switch (status) {
            case "ACTIVE" -> "Activo";
            case "RESOLVED" -> "Resuelto";
            case "CHRONIC" -> "Crónico";
            default -> status;
        };
    }
}
