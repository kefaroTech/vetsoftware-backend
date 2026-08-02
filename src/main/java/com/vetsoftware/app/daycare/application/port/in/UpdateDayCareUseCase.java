package com.vetsoftware.app.daycare.application.port.in;

import com.vetsoftware.app.daycare.application.command.UpdateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateDayCareUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('dayCare.update')")
  DayCareDto execute(UpdateDayCareCommand command);
}
