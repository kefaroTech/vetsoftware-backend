package com.vetsoftware.app.product.application.usecase;

import com.vetsoftware.app.product.application.command.SearchProductsCommand;
import com.vetsoftware.app.product.application.dto.PageResult;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.in.SearchProductsUseCase;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "product.search")
@Service
public class SearchProductsService implements SearchProductsUseCase {
  private final ProductRepository repository;

  public SearchProductsService(ProductRepository repository) {
    this.repository = repository;
  }

  @Override
  public PageResult<ProductDto> execute(SearchProductsCommand command) {
    return repository.search(command).map(ProductDto::from);
  }
}
