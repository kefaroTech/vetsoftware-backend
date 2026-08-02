package com.vetsoftware.app.goodsreceipt.application.usecase;

import com.vetsoftware.app.goodsreceipt.application.command.SearchGoodsReceiptsCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.dto.PageResult;
import com.vetsoftware.app.goodsreceipt.application.port.in.SearchGoodsReceiptsUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "goods.receipt.search")
@Service
public class SearchGoodsReceiptsService implements SearchGoodsReceiptsUseCase {
    private final GoodsReceiptRepository repository;

    public SearchGoodsReceiptsService(GoodsReceiptRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GoodsReceiptDto> execute(SearchGoodsReceiptsCommand command) {
        // readOnly tx: la query paginada trae las cabeceras y el mapper hidrata las
        // líneas LAZY aquí
        // dentro.
        return repository.search(command).map(GoodsReceiptDto::from);
    }
}
