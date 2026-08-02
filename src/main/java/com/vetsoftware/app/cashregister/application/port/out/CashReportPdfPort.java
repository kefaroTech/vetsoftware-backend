package com.vetsoftware.app.cashregister.application.port.out;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;

/** Renderiza el arqueo a PDF. */
public interface CashReportPdfPort {
  byte[] renderArqueo(CashArqueoReport report);
}
