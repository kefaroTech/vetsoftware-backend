package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * F4: emite un documento electrónico desde una cuenta cerrada de extremo a extremo (construir +
 * transmitir + entregar PDF/QR/correo si valida). El tipo (FE_VENTA / DOC_EQUIV_POS) lo elige el
 * caller.
 */
public interface EmitElectronicDocumentFromAccountUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('pos.create') and"
          + " @authz.isMyCompany(#command.companyId))")
  ElectronicDocumentDto execute(EmitElectronicDocumentCommand command);
}
