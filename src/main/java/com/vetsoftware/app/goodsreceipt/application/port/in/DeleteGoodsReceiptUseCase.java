package com.vetsoftware.app.goodsreceipt.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteGoodsReceiptUseCase {
    @PreAuthorize("hasAuthority('admin.all') or "
        + "(hasAuthority('goodsReceipt.create') and @authz.isMyCompany(#companyId))")
    void execute(Long id, Long companyId);
}
