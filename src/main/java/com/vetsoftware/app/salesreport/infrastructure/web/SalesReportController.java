package com.vetsoftware.app.salesreport.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.salesreport.application.dto.ReconciliationDto;
import com.vetsoftware.app.salesreport.application.dto.SalesBookDto;
import com.vetsoftware.app.salesreport.application.port.in.GetReconciliationUseCase;
import com.vetsoftware.app.salesreport.application.port.in.GetSalesBookUseCase;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** F6 - reportes contables de solo lectura: libro/registro de ventas y conciliación DIAN. */
@RestController
@RequestMapping("/sales-reports")
public class SalesReportController {
    private final GetSalesBookUseCase salesBookUseCase;
    private final GetReconciliationUseCase reconciliationUseCase;
    private final Authz authz;

    public SalesReportController(GetSalesBookUseCase salesBookUseCase,
                                 GetReconciliationUseCase reconciliationUseCase,
                                 Authz authz) {
        this.salesBookUseCase = salesBookUseCase;
        this.reconciliationUseCase = reconciliationUseCase;
        this.authz = authz;
    }

    @GetMapping("/sales-book")
    public SalesBookDto salesBook(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return salesBookUseCase.get(authz.currentCompanyId(), from, to);
    }

    @GetMapping("/reconciliation")
    public ReconciliationDto reconciliation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reconciliationUseCase.get(authz.currentCompanyId(), from, to);
    }
}
