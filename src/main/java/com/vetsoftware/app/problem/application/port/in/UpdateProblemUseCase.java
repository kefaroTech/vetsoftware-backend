package com.vetsoftware.app.problem.application.port.in;

import com.vetsoftware.app.problem.application.command.UpdateProblemCommand;
import com.vetsoftware.app.problem.application.dto.ProblemDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateProblemUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('animal.create') and"
            + " @authz.isMyCompany(#command.companyId))")
    ProblemDto execute(UpdateProblemCommand command);
}
