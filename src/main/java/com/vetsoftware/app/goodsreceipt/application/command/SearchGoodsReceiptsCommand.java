package com.vetsoftware.app.goodsreceipt.application.command;

import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import java.time.LocalDate;

public record SearchGoodsReceiptsCommand(Long companyId, Long supplierId, Long branchId,
        GoodsReceiptStatus status, LocalDate from, LocalDate to, int page, int pageSize) {
}
