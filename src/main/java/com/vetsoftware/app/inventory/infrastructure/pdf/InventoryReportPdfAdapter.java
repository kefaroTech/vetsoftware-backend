package com.vetsoftware.app.inventory.infrastructure.pdf;

import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;
import com.vetsoftware.app.inventory.application.port.out.InventoryReportPdfPort;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Renderiza los reportes de inventario con la infraestructura PDF embebida. */
@Component
public class InventoryReportPdfAdapter implements InventoryReportPdfPort {

    private final HtmlPdfRenderer renderer;

    public InventoryReportPdfAdapter(HtmlPdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public byte[] renderKardex(KardexReport report) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("r", report);
        return renderer.render("inventory-kardex", ctx);
    }

    @Override
    public byte[] renderPurchases(PurchasesReport report) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("r", report);
        return renderer.render("inventory-purchases", ctx);
    }
}
