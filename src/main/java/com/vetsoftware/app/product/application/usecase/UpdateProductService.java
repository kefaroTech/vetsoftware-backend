package com.vetsoftware.app.product.application.usecase;

import com.vetsoftware.app.product.application.command.UpdateProductCommand;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.in.UpdateProductUseCase;
import com.vetsoftware.app.product.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.product.application.port.out.ProductCategoryQueryPort;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.application.port.out.TaxQueryPort;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.ProductCodeAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNameAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNotFoundException;
import com.vetsoftware.app.product.domain.TaxRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product.update")
@Service
public class UpdateProductService implements UpdateProductUseCase {
    private final ProductRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final ProductCategoryQueryPort productCategoryQueryPort;
    private final TaxQueryPort taxQueryPort;

    public UpdateProductService(ProductRepository repository,
                                CompanyQueryPort companyQueryPort,
                                ProductCategoryQueryPort productCategoryQueryPort,
                                TaxQueryPort taxQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.productCategoryQueryPort = productCategoryQueryPort;
        this.taxQueryPort = taxQueryPort;
    }

    @Override
    @Transactional
    public ProductDto execute(UpdateProductCommand command) {
        Product product = repository.findById(command.id())
            .orElseThrow(() -> new ProductNotFoundException(command.id()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        if (repository.existsByCompanyIdAndCodeExcludingId(command.companyId(), command.code(), command.id())) {
            throw new ProductCodeAlreadyExistsException(command.code());
        }
        if (repository.existsByCompanyIdAndNameExcludingId(command.companyId(), command.name(), command.id())) {
            throw new ProductNameAlreadyExistsException(command.name());
        }
        ProductCategoryRef productCategory = productCategoryQueryPort.findById(command.productCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("ProductCategory not found: " + command.productCategoryId()));
        TaxRef tax = command.taxId() == null ? null
            : taxQueryPort.findById(command.taxId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Tax not found: " + command.taxId()));

        product.update(
            command.name(), command.code(), command.purchasePrice(), command.salePrice(),
            command.currentStock(), command.minStock(), command.provider(),
            command.taxTreatment(), command.expireDate(), command.notes(), productCategory, tax, company,
            command.updatedBy());
        return ProductDto.from(repository.save(product));
    }
}
