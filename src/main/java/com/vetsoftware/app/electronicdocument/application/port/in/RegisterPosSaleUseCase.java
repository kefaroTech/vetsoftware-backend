package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Registra una venta de POS como documento electronico. Requiere {@code electronicDocument.create} (emitir
 * documentos), no solo lectura del catalogo: emitir un documento fiscal es una operacion de facturacion. El
 * gate del modulo BILLING (transmision a la DIAN) es independiente: sin el modulo el documento igual se
 * persiste PENDIENTE. Las lineas GENERAL (precio libre, sin validar contra catalogo) solo las puede emitir
 * {@code admin.all}; el resto valida el precio contra el catalogo en el builder.
 */
public interface RegisterPosSaleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('pos.create') "
            + "and @authz.isMyCompany(#command.companyId) and !#command.hasGeneralLine())")
    ElectronicDocumentDto execute(RegisterPosSaleCommand command);
}
