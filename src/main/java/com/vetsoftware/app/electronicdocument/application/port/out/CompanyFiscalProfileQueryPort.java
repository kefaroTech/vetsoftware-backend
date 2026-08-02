package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lee el perfil fiscal del emisor (CompanyTaxProfile) + sus tarifas de retencion
 * (WithholdingConfig) y los traduce al read model de esta feature. Unico punto que conoce esas
 * otras features para el emisor. Reutilizado tanto por el flujo de cuenta cerrada como por la venta
 * directa de POS.
 */
public interface CompanyFiscalProfileQueryPort {
  Optional<CompanyFiscalProfile> findByCompany(Long companyId);

  /** Snapshot del emisor + tarifas de retencion del emisor (null si no hay WithholdingConfig). */
  record CompanyFiscalProfile(
      IssuerSnapshot issuer,
      BigDecimal reteFuenteRate,
      BigDecimal reteIvaRate,
      BigDecimal reteIcaRate) {}
}
