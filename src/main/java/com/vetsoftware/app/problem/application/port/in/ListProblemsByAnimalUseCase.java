package com.vetsoftware.app.problem.application.port.in;

import com.vetsoftware.app.problem.application.dto.ProblemDto;
import com.vetsoftware.app.problem.application.dto.PageResult;
import com.vetsoftware.app.problem.application.query.ListProblemsByAnimalQuery;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProblemsByAnimalUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.read') and @authz.isMyCompany(#query.companyId))")
    PageResult<ProblemDto> execute(ListProblemsByAnimalQuery query);
}
