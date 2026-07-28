package com.vetsoftware.app.inventory.application.port.out;

import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;

/** Renderiza los reportes de inventario a PDF. */
public interface InventoryReportPdfPort {
    byte[] renderKardex(KardexReport report);
    byte[] renderPurchases(PurchasesReport report);
}
