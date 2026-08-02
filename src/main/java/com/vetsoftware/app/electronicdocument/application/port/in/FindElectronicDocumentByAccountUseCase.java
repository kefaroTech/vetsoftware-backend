package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Lee el documento electronico emitido al cerrar una cuenta (para imprimir su recibo). Disponible
 * para quien gestiona la cuenta (openAccount.read) o el modulo de facturacion
 * (electronicDocument.read). Devuelve vacio cuando la cuenta no genero documento (cancelada o
 * cobrada sin el modulo de facturacion).
 */
public interface FindElectronicDocumentByAccountUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or "
          + "((hasAuthority('pos.read') or hasAuthority('pos.read')) "
          + "and @authz.isMyCompany(#companyId))")
  Optional<ElectronicDocumentDto> findByOpenAccount(Long openAccountId, Long companyId);
}
