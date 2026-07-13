package com.vetsoftware.app.purchasereport.infrastructure.pdf;

import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import com.vetsoftware.app.infrastructure.pdf.PdfOptions;
import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import com.vetsoftware.app.purchasereport.application.port.out.PurchaseBookPdfPort;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Renderiza el libro de compras a PDF vía la infra compartida (Thymeleaf + Gotenberg). */
@Component
public class PurchaseBookGotenbergAdapter implements PurchaseBookPdfPort {

    private final HtmlPdfRenderer renderer;

    public PurchaseBookGotenbergAdapter(HtmlPdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public byte[] renderPurchaseBook(PurchaseBookDto book) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("b", book);
        return renderer.render("purchase-book", ctx, PdfOptions.defaults());
    }
}
