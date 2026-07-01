package com.vetsoftware.app.product.application.usecase;

import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.in.ReactivateProductUseCase;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product.reactivate")
@Service
public class ReactivateProductService implements ReactivateProductUseCase {
    private final ProductRepository repository;

    public ReactivateProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ProductDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0) throw new ProductNotFoundException(id);
        return ProductDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ProductNotFoundException(id)));
    }
}
