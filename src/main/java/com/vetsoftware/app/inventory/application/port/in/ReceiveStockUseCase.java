package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import org.springframework.security.access.prepost.PreAuthorize;

/** Entrada de mercancía (recepción/compra): crea/acumula lote con costo y vencimiento en la sede. */
public interface ReceiveStockUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('inventory.adjust') and @authz.isMyCompany(#command.companyId))")
    void receive(RecordPurchaseCommand command);
}
