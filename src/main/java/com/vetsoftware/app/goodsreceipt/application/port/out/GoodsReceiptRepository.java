package com.vetsoftware.app.goodsreceipt.application.port.out;

import com.vetsoftware.app.goodsreceipt.application.command.SearchGoodsReceiptsCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.PageResult;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import java.util.List;
import java.util.Optional;

public interface GoodsReceiptRepository {
    GoodsReceipt save(GoodsReceipt goodsReceipt);

    Optional<GoodsReceipt> findByIdAndCompanyId(Long id, Long companyId);

    List<GoodsReceipt> findAllByCompanyId(Long companyId);

    PageResult<GoodsReceipt> search(SearchGoodsReceiptsCommand command);

    void delete(Long id);
}
