package com.vetsoftware.app.goodsreceipt.application.port.in;

import com.vetsoftware.app.goodsreceipt.application.command.SearchGoodsReceiptsCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SearchGoodsReceiptsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('goodsReceipt.read') and @authz.isMyCompany(#command.companyId))")
    PageResult<GoodsReceiptDto> execute(SearchGoodsReceiptsCommand command);
}
