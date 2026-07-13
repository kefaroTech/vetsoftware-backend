package com.vetsoftware.app.supplierinvoice.application.command;

import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import java.time.LocalDate;

public record SearchSupplierInvoicesCommand(
        Long companyId,
        Long supplierId,
        Long branchId,
        SupplierInvoiceStatus status,
        LocalDate from,
        LocalDate to,
        int page,
        int pageSize
) {}
