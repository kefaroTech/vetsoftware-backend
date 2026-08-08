package com.vetsoftware.app.problem.application.port.out;

import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.application.dto.PageResult;
import java.util.Optional;

public interface ProblemRepository {
    Problem save(Problem problem);

    Optional<Problem> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<Problem> findByAnimalIdAndCompanyId(Long animalId, Long companyId, int page,
            int pageSize);

    void delete(Long id, Long companyId);
}
