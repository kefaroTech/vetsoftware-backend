package com.vetsoftware.app.cashregister.infrastructure.pdf;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;
import com.vetsoftware.app.cashregister.application.port.out.CashReportPdfPort;
import com.vetsoftware.app.infrastructure.pdf.HtmlPdfRenderer;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Renderiza el arqueo con la infraestructura PDF embebida. */
@Component
public class CashReportPdfAdapter implements CashReportPdfPort {

  private final HtmlPdfRenderer renderer;

  public CashReportPdfAdapter(HtmlPdfRenderer renderer) {
    this.renderer = renderer;
  }

  @Override
  public byte[] renderArqueo(CashArqueoReport report) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put("r", report);
    return renderer.render("cash-arqueo", ctx);
  }
}
