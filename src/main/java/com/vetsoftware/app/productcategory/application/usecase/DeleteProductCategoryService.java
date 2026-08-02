package com.vetsoftware.app.productcategory.application.usecase;

import com.vetsoftware.app.productcategory.application.port.in.DeleteProductCategoryUseCase;
import com.vetsoftware.app.productcategory.application.port.out.ProductCategoryRepository;
import com.vetsoftware.app.productcategory.application.port.out.ProductChildrenQueryPort;
import com.vetsoftware.app.productcategory.domain.ProductCategoryHasActiveChildrenException;
import com.vetsoftware.app.productcategory.domain.ProductCategoryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product.category.delete")
@Service
public class DeleteProductCategoryService implements DeleteProductCategoryUseCase {
  private final ProductCategoryRepository repository;
  private final ProductChildrenQueryPort productChildrenQueryPort;

  public DeleteProductCategoryService(
      ProductCategoryRepository repository, ProductChildrenQueryPort productChildrenQueryPort) {
    this.repository = repository;
    this.productChildrenQueryPort = productChildrenQueryPort;
  }

  @Override
  @Transactional
  public void execute(Long id, Long companyId) {
    repository
        .findByIdAndCompanyId(id, companyId)
        .orElseThrow(() -> new ProductCategoryNotFoundException(id));
    if (productChildrenQueryPort.existsActiveByProductCategoryId(id)) {
      throw new ProductCategoryHasActiveChildrenException(id, "product");
    }
    repository.delete(id);
  }
}
