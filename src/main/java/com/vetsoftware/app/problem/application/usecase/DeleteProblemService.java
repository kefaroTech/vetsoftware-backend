package com.vetsoftware.app.problem.application.usecase;

import com.vetsoftware.app.problem.application.port.in.DeleteProblemUseCase;
import com.vetsoftware.app.problem.application.port.out.ProblemRepository;
import com.vetsoftware.app.problem.domain.ProblemNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "problem.delete")
@Service
public class DeleteProblemService implements DeleteProblemUseCase {
  private final ProblemRepository repository;

  public DeleteProblemService(ProblemRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id, Long companyId) {
    repository
        .findByIdAndCompanyId(id, companyId)
        .orElseThrow(() -> new ProblemNotFoundException(id));
    repository.delete(id, companyId);
  }
}
