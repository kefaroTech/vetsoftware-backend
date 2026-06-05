package com.vetsoftware.app.product.application.port.in;

import com.vetsoftware.app.product.application.command.CreateProductCommand;
import com.vetsoftware.app.product.application.dto.ProductDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateProductUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('product.create') and @authz.isMyCompany(#command.companyId))")
    ProductDto execute(CreateProductCommand command);
}
