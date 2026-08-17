package com.vetsoftware.app.productchargeopenaccount.application.port.in;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProductChargeOpenAccountsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<ProductChargeOpenAccountDto> listAll(Long companyId, int page, int pageSize);
}
