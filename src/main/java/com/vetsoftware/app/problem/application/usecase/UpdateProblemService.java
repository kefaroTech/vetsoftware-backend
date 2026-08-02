package com.vetsoftware.app.problem.application.usecase;

import com.vetsoftware.app.problem.application.command.UpdateProblemCommand;
import com.vetsoftware.app.problem.application.dto.ProblemDto;
import com.vetsoftware.app.problem.application.port.in.UpdateProblemUseCase;
import com.vetsoftware.app.problem.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.problem.application.port.out.ProblemRepository;
import com.vetsoftware.app.problem.domain.CompanyRef;
import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.domain.ProblemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "problem.update")
@Service
public class UpdateProblemService implements UpdateProblemUseCase {
  private final ProblemRepository repository;
  private final CompanyQueryPort companyQueryPort;

  public UpdateProblemService(ProblemRepository repository, CompanyQueryPort companyQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
  }

  @Override
  @Transactional
  public ProblemDto execute(UpdateProblemCommand command) {
    Problem problem =
        repository
            .findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new ProblemNotFoundException(command.id()));
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
    problem.update(
        command.description(),
        command.status(),
        command.onsetDate(),
        command.resolvedDate(),
        command.notes(),
        company);
    return ProblemDto.from(repository.save(problem));
  }
}
