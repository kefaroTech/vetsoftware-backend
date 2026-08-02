package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Auto-emisión disparada al CERRAR/cobrar una cuenta (no es la emisión manual). A diferencia de
 * {@link EmitElectronicDocumentFromAccountUseCase}, NO exige el permiso `electronicDocument.emit`:
 * el derecho a emitir es consecuencia de haber cerrado la venta (ya autorizado por
 * `openAccount.update`). Solo se exige pertenencia a la empresa (defensa en profundidad).
 * Idempotente: no duplica si la cuenta ya tiene documento. Devuelve null si no emite (cuenta ya
 * facturada).
 */
public interface EmitElectronicDocumentOnCloseUseCase {
  @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#command.companyId)")
  ElectronicDocumentDto execute(EmitElectronicDocumentCommand command);
}
