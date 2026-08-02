package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.ConvertPosToInvoiceCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * F4: emite una factura electrónica de venta (FE) a partir de un documento
 * equivalente POS previo.
 */
public interface ConvertPosToInvoiceUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('pos.create') and"
            + " @authz.isMyCompany(#command.companyId))")
    ElectronicDocumentDto execute(ConvertPosToInvoiceCommand command);
}
