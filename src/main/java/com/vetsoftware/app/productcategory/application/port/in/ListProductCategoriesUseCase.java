package com.vetsoftware.app.productcategory.application.port.in;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProductCategoriesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or @authz.isMyCompany(#companyId)")
    List<ProductCategoryDto> listByCompany(Long companyId);
}
