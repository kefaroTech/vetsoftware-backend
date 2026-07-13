package com.vetsoftware.app.goodsreceipt.application.usecase;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.in.ListGoodsReceiptsUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "goods_receipt.list_by_company")
@Service
public class ListGoodsReceiptsService implements ListGoodsReceiptsUseCase {
    private final GoodsReceiptRepository repository;

    public ListGoodsReceiptsService(GoodsReceiptRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceiptDto> listByCompany(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(GoodsReceiptDto::from).toList();
    }
}
