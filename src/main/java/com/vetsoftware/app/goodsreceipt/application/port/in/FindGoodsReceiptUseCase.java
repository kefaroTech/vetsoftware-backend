package com.vetsoftware.app.goodsreceipt.application.port.in;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindGoodsReceiptUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('goodsReceipt.read') and @authz.isMyCompany(#companyId))")
    GoodsReceiptDto findById(Long id, Long companyId);
}
