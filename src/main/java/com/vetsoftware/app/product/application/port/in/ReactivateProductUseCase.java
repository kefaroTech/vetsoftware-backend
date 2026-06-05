package com.vetsoftware.app.product.application.port.in;

import com.vetsoftware.app.product.application.dto.ProductDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateProductUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('product.delete')")
    ProductDto execute(Long id);
}
