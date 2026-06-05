package com.vetsoftware.app.product.application.usecase;

import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.in.FindProductUseCase;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "product.find")
@Service
public class FindProductService implements FindProductUseCase {
    private final ProductRepository repository;

    public FindProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductDto findById(Long id) {
        return ProductDto.from(repository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id)));
    }
}
