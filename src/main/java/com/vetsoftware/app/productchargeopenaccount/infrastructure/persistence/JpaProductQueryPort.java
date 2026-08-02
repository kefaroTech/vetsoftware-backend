package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.product.domain.TaxTreatment;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import com.vetsoftware.app.productchargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("productChargeOpenAccountJpaProductQueryPort")
public class JpaProductQueryPort implements ProductQueryPort {
  private final ProductJpaRepository productJpaRepository;

  public JpaProductQueryPort(ProductJpaRepository productJpaRepository) {
    this.productJpaRepository = productJpaRepository;
  }

  @Override
  public Optional<ProductRef> findByIdAndCompanyId(Long productId, Long companyId) {
    return productJpaRepository
        .findByIdAndCompany_Id(productId, companyId)
        .map(JpaProductQueryPort::toRef);
  }

  private static ProductRef toRef(ProductJpaEntity e) {
    // Gravado tanto GRAVADO (IVA) como INC: ambos llevan impuesto. El esquema (IVA/INC) sale del
    // Tax y
    // se congela en el cargo para que el documento del cierre lo respete igual que la venta POS.
    TaxTreatment treatment = e.getTaxTreatment();
    boolean hasTax = treatment == TaxTreatment.GRAVADO || treatment == TaxTreatment.INC;
    TaxJpaEntity t = e.getTax();
    TaxRef tax =
        hasTax && t != null
            ? new TaxRef(
                t.getId(),
                t.getName(),
                t.getPercentage(),
                t.getTaxScheme() == null ? null : t.getTaxScheme().name())
            : null;
    // Congela el tratamiento real del catálogo (incl. EXENTO/EXCLUIDO), no solo el hasTax
    // monetario.
    return new ProductRef(
        e.getId(),
        e.getName(),
        e.getCode(),
        e.getSalePrice(),
        hasTax,
        tax,
        treatment == null ? null : treatment.name());
  }
}
