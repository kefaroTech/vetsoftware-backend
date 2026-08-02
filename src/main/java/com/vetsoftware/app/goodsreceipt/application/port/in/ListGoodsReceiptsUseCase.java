package com.vetsoftware.app.goodsreceipt.application.port.in;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListGoodsReceiptsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('goodsReceipt.read') and @authz.isMyCompany(#companyId))")
    List<GoodsReceiptDto> listByCompany(Long companyId);
}
