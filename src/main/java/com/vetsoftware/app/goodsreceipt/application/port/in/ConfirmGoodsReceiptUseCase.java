package com.vetsoftware.app.goodsreceipt.application.port.in;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ConfirmGoodsReceiptUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('goodsReceipt.create') and @authz.isMyCompany(#companyId))")
    GoodsReceiptDto execute(Long id, Long companyId, Long actorId);
}
