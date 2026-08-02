package com.vetsoftware.app.product.application.port.in;

import com.vetsoftware.app.product.application.command.UpdateProductCommand;
import com.vetsoftware.app.product.application.dto.ProductDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateProductUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('product.update') and @authz.isMyCompany(#command.companyId))")
    ProductDto execute(UpdateProductCommand command);
}
