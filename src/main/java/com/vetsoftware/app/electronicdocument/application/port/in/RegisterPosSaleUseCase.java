package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Registra una venta de POS como documento electronico. Disponible para cualquier usuario con acceso al
 * POS ({@code product.read}); NO se gatea con permisos de facturacion electronica porque las empresas SIN
 * el modulo tambien deben persistir la venta (el documento queda PENDIENTE). La transmision a la DIAN la
 * decide el backend segun el derecho BILLING de la empresa.
 */
public interface RegisterPosSaleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('product.read') and @authz.isMyCompany(#command.companyId))")
    ElectronicDocumentDto execute(RegisterPosSaleCommand command);
}
