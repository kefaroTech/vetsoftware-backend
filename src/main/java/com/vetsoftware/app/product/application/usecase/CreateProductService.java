package com.vetsoftware.app.product.application.usecase;

import com.vetsoftware.app.product.application.command.CreateProductCommand;
import com.vetsoftware.app.product.application.dto.ProductDto;
import com.vetsoftware.app.product.application.port.in.CreateProductUseCase;
import com.vetsoftware.app.product.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.product.application.port.out.DefaultProductPresentationPort;
import com.vetsoftware.app.product.application.port.out.ProductCategoryQueryPort;
import com.vetsoftware.app.product.application.port.out.ProductRepository;
import com.vetsoftware.app.product.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.product.application.port.out.TaxQueryPort;
import com.vetsoftware.app.product.application.port.out.UnitMeasureQueryPort;
import com.vetsoftware.app.product.domain.CompanyRef;
import com.vetsoftware.app.product.domain.Product;
import com.vetsoftware.app.product.domain.ProductCategoryRef;
import com.vetsoftware.app.product.domain.ProductCodeAlreadyExistsException;
import com.vetsoftware.app.product.domain.ProductNameAlreadyExistsException;
import com.vetsoftware.app.product.domain.SupplierRef;
import com.vetsoftware.app.product.domain.TaxRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product.create")
@Service
public class CreateProductService implements CreateProductUseCase {
    private final ProductRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final ProductCategoryQueryPort productCategoryQueryPort;
    private final TaxQueryPort taxQueryPort;
    private final SupplierQueryPort supplierQueryPort;
    private final UnitMeasureQueryPort unitMeasureQueryPort;
    private final DefaultProductPresentationPort defaultPresentationPort;

    public CreateProductService(ProductRepository repository, CompanyQueryPort companyQueryPort,
            ProductCategoryQueryPort productCategoryQueryPort, TaxQueryPort taxQueryPort,
            SupplierQueryPort supplierQueryPort, UnitMeasureQueryPort unitMeasureQueryPort,
            DefaultProductPresentationPort defaultPresentationPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.productCategoryQueryPort = productCategoryQueryPort;
        this.taxQueryPort = taxQueryPort;
        this.supplierQueryPort = supplierQueryPort;
        this.unitMeasureQueryPort = unitMeasureQueryPort;
        this.defaultPresentationPort = defaultPresentationPort;
    }

    @Override
    @Transactional
    public ProductDto execute(CreateProductCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        if (repository.existsByCompanyIdAndCode(command.companyId(), command.code())) {
            throw new ProductCodeAlreadyExistsException(command.code());
        }
        if (repository.existsByCompanyIdAndName(command.companyId(), command.name())) {
            throw new ProductNameAlreadyExistsException(command.name());
        }
        ProductCategoryRef productCategory = productCategoryQueryPort
                .findById(command.productCategoryId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "ProductCategory not found: " + command.productCategoryId()));
        TaxRef tax = command.taxId() == null
                ? null
                : taxQueryPort.findById(command.taxId(), command.companyId()).orElseThrow(
                        () -> new IllegalArgumentException("Tax not found: " + command.taxId()));
        SupplierRef supplier = command.supplierId() == null
                ? null
                : supplierQueryPort.findById(command.supplierId(), command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Supplier not found: " + command.supplierId()));
        if (!unitMeasureQueryPort.exists(command.baseUnitMeasureCode())) {
            throw new IllegalArgumentException(
                    "Unit measure not found: " + command.baseUnitMeasureCode());
        }

        Product product = Product.create(command.name(), command.code(), command.salePrice(),
                command.baseUnitMeasureCode(), command.provider(), supplier, command.taxTreatment(),
                command.notes(), productCategory, tax, company);
        Product saved = repository.save(product);
        defaultPresentationPort.ensureDefault(saved.getId(), command.companyId(),
                command.baseUnitMeasureCode(), command.salePrice());
        return ProductDto.from(saved);
    }
}
