package com.vetsoftware.app.product.application.port.in;

import com.vetsoftware.app.product.application.dto.ProductDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindProductUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or " + "(hasAuthority('product.read') and @authz.isMyCompany(#companyId))")
  ProductDto findById(Long id, Long companyId);
}
