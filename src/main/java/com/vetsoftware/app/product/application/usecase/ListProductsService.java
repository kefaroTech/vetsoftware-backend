package com.vetsoftware.app.product.application.usecase;

import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.in.ListProductsUseCase;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product.list.by.company")
@Service
public class ListProductsService implements ListProductsUseCase {
  private final ProductRepository repository;

  public ListProductsService(ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ProductDto> listByCompany(Long companyId) {
    return repository.findAllByCompanyId(companyId).stream().map(ProductDto::from).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductDto> listDisabledByCompany(Long companyId) {
    // readOnly tx: la query nativa trae los pausados y el mapper hidrata sus asociaciones LAZY aquí
    // dentro.
    return repository.findAllDisabledByCompanyId(companyId).stream().map(ProductDto::from).toList();
  }
}
