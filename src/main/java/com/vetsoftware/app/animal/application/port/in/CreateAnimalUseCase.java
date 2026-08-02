package com.vetsoftware.app.animal.application.port.in;

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateAnimalUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('animal.create') and"
          + " @authz.isMyCompany(#command.companyId))")
  AnimalDto execute(CreateAnimalCommand command);
}
