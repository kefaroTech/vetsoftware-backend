package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.clinicalhistory.application.dto.ReportProblem;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalProblemsQueryPort;
import com.vetsoftware.app.problem.infrastructure.persistence.ProblemJpaRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
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

    /**
     * La ficha clinica muestra la lista de problemas del animal completa, pero
     * acotada: es finita por definicion y este tope evita que un historial largo se
     * traiga miles de filas.
     */
    private static final Pageable ANIMAL_PROBLEMS_PAGE = Pageable.ofSize(200);

    private final ProblemJpaRepository problemJpaRepository;

    public JpaAnimalProblemsQueryPort(ProblemJpaRepository problemJpaRepository) {
        this.problemJpaRepository = problemJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportProblem> findByAnimal(Long animalId, Long companyId) {
        return problemJpaRepository.findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(animalId,
                companyId, ANIMAL_PROBLEMS_PAGE).getContent().stream().map(e -> {
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
