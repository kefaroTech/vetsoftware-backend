package com.vetsoftware.app.supplierinvoice.application.port.in;

import com.vetsoftware.app.supplierinvoice.application.command.SearchSupplierInvoicesCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.PageResult;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchSupplierInvoicesUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "(hasAuthority('supplierinvoice.read') and @authz.isMyCompany(#command.companyId))")
  PageResult<SupplierInvoiceDto> execute(SearchSupplierInvoicesCommand command);
}
