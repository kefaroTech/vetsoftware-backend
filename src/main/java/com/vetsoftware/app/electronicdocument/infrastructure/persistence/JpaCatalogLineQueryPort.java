package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.CatalogLineQueryPort;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaEntity;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter: deriva la clasificacion tributaria de la linea desde el catalogo (filtrado por enabled=true via
 * @SQLRestriction). taxTreatment espeja 1:1 a TaxCategory; el esquema/tasa salen del impuesto asociado.
 * GRAVADO/INC con impuesto y tasa > 0 -> (categoria, esquema del tax, tasa); EXENTO/EXCLUIDO -> sin tributo.
 */
@Component
public class JpaCatalogLineQueryPort implements CatalogLineQueryPort {
    private final ProductJpaRepository productRepository;
    private final ServiceJpaRepository serviceRepository;

    public JpaCatalogLineQueryPort(ProductJpaRepository productRepository,
                                   ServiceJpaRepository serviceRepository) {
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
    }

    @Override
    public Optional<CatalogItem> findProduct(Long productId, Long companyId) {
        return productRepository.findById(productId)
                .filter(p -> p.getCompany() != null && companyId.equals(p.getCompany().getId()))
                .map(p -> toItem(p.getName(), p.getTaxTreatment().name(), p.getTax(),
                        p.getSalePrice(), p.getProductCategory().getId()));
    }

    @Override
    public Optional<CatalogItem> findService(Long serviceId, Long companyId) {
        return serviceRepository.findById(serviceId)
                .filter(s -> s.getCompany() != null && companyId.equals(s.getCompany().getId()))
                .map(s -> toItem(s.getName(), s.getTaxTreatment().name(), s.getTax(),
                        s.getPrice(), s.getServiceCategory().getId()));
    }

    private CatalogItem toItem(String name, String taxTreatment, TaxJpaEntity tax,
                               BigDecimal basePrice, Long categoryId) {
        TaxCategory category = TaxCategory.valueOf(taxTreatment); // product/service.TaxTreatment espeja TaxCategory
        boolean taxed = (category == TaxCategory.GRAVADO || category == TaxCategory.INC)
                && tax != null && tax.getPercentage() != null && tax.getPercentage().signum() > 0;
        if (!taxed) {
            return new CatalogItem(name, category, null, null, basePrice, categoryId);
        }
        TaxScheme scheme = TaxScheme.valueOf(tax.getTaxScheme().name()); // tax.TaxScheme (IVA/INC) -> doc.TaxScheme
        return new CatalogItem(name, category, scheme, tax.getPercentage(), basePrice, categoryId);
    }
}
