package com.vetsoftware.app.product.application.usecase;

import com.vetsoftware.app.product.application.port.in.DeleteProductUseCase;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product.delete")
@Service
public class DeleteProductService implements DeleteProductUseCase {
    private final ProductRepository repository;

    public DeleteProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ProductNotFoundException(id));
        repository.delete(id);
    }
}
