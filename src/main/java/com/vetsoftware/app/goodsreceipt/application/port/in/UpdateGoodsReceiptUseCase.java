package com.vetsoftware.app.goodsreceipt.application.port.in;

import com.vetsoftware.app.goodsreceipt.application.command.UpdateGoodsReceiptCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateGoodsReceiptUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('goodsReceipt.create') and @authz.isMyCompany(#command.companyId))")
    GoodsReceiptDto execute(UpdateGoodsReceiptCommand command);
}
