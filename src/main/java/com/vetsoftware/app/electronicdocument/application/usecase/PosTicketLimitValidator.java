package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.UvtQueryPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 3.2 — Control del tope de 5 UVT del documento equivalente POS (Res. DIAN 000165/2023): el
 * DOC_EQUIV_POS solo puede emitirse para ventas cuyo total (payableAmount) sea ≤ 5·UVT. Por encima
 * hay que emitir Factura electrónica (FE_VENTA) con el adquiriente identificado. Enforcement de
 * backend (defensa en profundidad frente al gating del front). Solo aplica a DOC_EQUIV_POS; el
 * resto de tipos no se toca.
 */
@Component
public class PosTicketLimitValidator {
  private static final BigDecimal MAX_UVT = BigDecimal.valueOf(5);

  private final UvtQueryPort uvtQueryPort;

  public PosTicketLimitValidator(UvtQueryPort uvtQueryPort) {
    this.uvtQueryPort = uvtQueryPort;
  }

  public void validate(ElectronicDocument document) {
    if (document.getDocumentType() != ElectronicDocumentType.DOC_EQUIV_POS) {
      return;
    }
    BigDecimal uvt =
        uvtQueryPort
            .currentUvt()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No hay UVT configurado (system_configurations, fila 'uvt'): no se puede"
                            + " validar el tope de 5 UVT del tiquete POS."));
    BigDecimal limit = uvt.multiply(MAX_UVT);
    if (document.getPayableAmount().compareTo(limit) > 0) {
      throw new IllegalArgumentException(
          "El documento equivalente POS ("
              + document.getPayableAmount()
              + ") supera el límite de 5 UVT ("
              + limit
              + "). Emita factura electrónica (FE_VENTA) con el adquiriente identificado.");
    }
  }
}
