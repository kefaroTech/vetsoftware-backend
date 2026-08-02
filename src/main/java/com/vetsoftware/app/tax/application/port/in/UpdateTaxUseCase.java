package com.vetsoftware.app.tax.application.port.in;

import com.vetsoftware.app.tax.application.command.UpdateTaxCommand;
import com.vetsoftware.app.tax.application.dto.TaxDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateTaxUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('tax.update') and"
          + " @authz.isMyCompany(#command.companyId))")
  TaxDto execute(UpdateTaxCommand command);
}
