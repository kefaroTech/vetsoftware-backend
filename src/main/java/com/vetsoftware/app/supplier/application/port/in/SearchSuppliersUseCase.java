package com.vetsoftware.app.supplier.application.port.in;

import com.vetsoftware.app.supplier.application.command.SearchSuppliersCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchSuppliersUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('supplier.read') and @authz.isMyCompany(#command.companyId))")
    PageResult<SupplierDto> execute(SearchSuppliersCommand command);
}
