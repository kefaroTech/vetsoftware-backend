package com.vetsoftware.app.supplierinvoice.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSupplierInvoiceRequest(
        Long branchId,
        @NotNull Long supplierId,
        Long purchaseOrderId,
        Long goodsReceiptId,
        @NotBlank @Size(max = 60) String invoiceNumber,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.0") BigDecimal subtotal,
        @NotNull @DecimalMin("0.0") BigDecimal taxAmount,
        @DecimalMin("0.0") BigDecimal withholdingAmount,
        @Size(max = 500) String notes
) {}
