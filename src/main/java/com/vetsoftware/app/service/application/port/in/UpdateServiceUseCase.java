package com.vetsoftware.app.service.application.port.in;

import com.vetsoftware.app.service.application.command.UpdateServiceCommand;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateServiceUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('service.update') and @authz.isMyCompany(#command.companyId))")
  ServiceDto execute(UpdateServiceCommand command);
}
