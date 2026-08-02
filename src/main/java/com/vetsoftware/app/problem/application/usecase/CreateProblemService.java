package com.vetsoftware.app.problem.application.usecase;

import com.vetsoftware.app.problem.application.command.CreateProblemCommand;
import com.vetsoftware.app.problem.application.dto.ProblemDto;
import com.vetsoftware.app.problem.application.port.in.CreateProblemUseCase;
import com.vetsoftware.app.problem.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.problem.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.problem.application.port.out.ProblemRepository;
import com.vetsoftware.app.problem.domain.AnimalRef;
import com.vetsoftware.app.problem.domain.CompanyRef;
import com.vetsoftware.app.problem.domain.Problem;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "problem.create")
@Service
public class CreateProblemService implements CreateProblemUseCase {
    private final ProblemRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateProblemService(ProblemRepository repository, AnimalQueryPort animalQueryPort,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public ProblemDto execute(CreateProblemCommand command) {
        AnimalRef animal = animalQueryPort
                .findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        Problem problem = Problem.create(animal, company, command.description(), command.status(),
                command.onsetDate(), command.resolvedDate(), command.notes());
        return ProblemDto.from(repository.save(problem));
    }
}
