package com.vetsoftware.app.prescription.application.port.in;

import com.vetsoftware.app.prescription.application.command.UpdatePrescriptionCommand;
import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdatePrescriptionUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('prescription.update') and @authz.isMyCompany(#command.companyId))")
  PrescriptionDto execute(UpdatePrescriptionCommand command);
}
