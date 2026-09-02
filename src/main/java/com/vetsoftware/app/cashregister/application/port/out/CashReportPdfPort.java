package com.vetsoftware.app.cashregister.application.port.out;

import com.vetsoftware.app.cashregister.application.dto.CashArqueoReport;

public interface CashReportPdfPort {
    byte[] renderArqueo(CashArqueoReport report);
}
