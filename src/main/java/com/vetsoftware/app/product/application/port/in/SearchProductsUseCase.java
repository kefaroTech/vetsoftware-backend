package com.vetsoftware.app.product.application.port.in;

import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.dto.ProductDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchProductsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('product.read') and @authz.isMyCompany(#command.companyId))")
    PageResult<ProductDto> execute(SearchProductsCommand command);
}
