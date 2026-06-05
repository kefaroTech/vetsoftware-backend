package com.vetsoftware.app.product.application.port.in;

import com.vetsoftware.app.product.application.dto.ProductDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindProductUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('product.read')")
    ProductDto findById(Long id);
}
