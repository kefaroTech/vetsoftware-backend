package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.BuildElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * F2: construye (no transmite) el documento desde una cuenta cerrada. Punto de entrada PROVISIONAL
 * para construir/probar el modelo; F4 reemplazara este disparador manual por la emision al cerrar.
 */
public interface BuildElectronicDocumentFromAccountUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('pos.create') and @authz.isMyCompany(#command.companyId))")
    ElectronicDocumentDto execute(BuildElectronicDocumentCommand command);
}
