package com.vetsoftware.app.productcategory.application.usecase;

import com.vetsoftware.app.productcategory.application.dto.ProductCategoryDto;
import com.vetsoftware.app.productcategory.application.port.in.ListProductCategoriesUseCase;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "product.category.list")
@Service
public class ListProductCategoriesService implements ListProductCategoriesUseCase {
  private final ProductCategoryRepository repository;

  public ListProductCategoriesService(ProductCategoryRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ProductCategoryDto> listByCompany(Long companyId) {
    return repository.findAllByCompanyId(companyId).stream().map(ProductCategoryDto::from).toList();
  }
}
