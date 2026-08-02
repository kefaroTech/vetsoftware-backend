package com.vetsoftware.app.purchasereport.infrastructure.pdf;

import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import com.vetsoftware.app.purchasereport.application.port.out.PurchaseBookPdfPort;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Renderiza el libro de compras con la infraestructura PDF embebida. */
@Component
public class PurchaseBookPdfAdapter implements PurchaseBookPdfPort {

  private final HtmlPdfRenderer renderer;

  public PurchaseBookPdfAdapter(HtmlPdfRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public byte[] renderPurchaseBook(PurchaseBookDto book) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put("b", book);
    return renderer.render("purchase-book", ctx);
  }
}
