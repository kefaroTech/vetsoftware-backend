package com.vetsoftware.app.problem.application.port.out;

import com.vetsoftware.app.problem.domain.Problem;
import java.util.List;
import java.util.Optional;

public interface ProblemRepository {
  Problem save(Problem problem);

  Optional<Problem> findByIdAndCompanyId(Long id, Long companyId);

  List<Problem> findByAnimalIdAndCompanyId(Long animalId, Long companyId);

  void delete(Long id, Long companyId);
}
