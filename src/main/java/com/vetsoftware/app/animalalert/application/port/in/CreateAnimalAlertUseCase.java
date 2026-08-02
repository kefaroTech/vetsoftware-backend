package com.vetsoftware.app.animalalert.application.port.in;

import com.vetsoftware.app.animalalert.application.command.CreateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateAnimalAlertUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('animal.create') and"
          + " @authz.isMyCompany(#command.companyId))")
  AnimalAlertDto execute(CreateAnimalAlertCommand command);
}
