package com.vetsoftware.app.supplierinvoice.application.port.in;

import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CancelSupplierInvoiceUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('supplierinvoice.update') and @authz.isMyCompany(#companyId))")
  SupplierInvoiceDto execute(Long id, Long companyId, Long actorId);
}
