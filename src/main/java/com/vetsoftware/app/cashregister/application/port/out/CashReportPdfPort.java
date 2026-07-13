package com.vetsoftware.app.cashregister.application.port.out;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;

/** Renderiza el arqueo a PDF (adapter Gotenberg + plantilla Thymeleaf). */
public interface CashReportPdfPort {
    byte[] renderArqueo(CashArqueoReport report);
}
