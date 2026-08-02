package com.vetsoftware.app.goodsreceipt.application.usecase;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.in.FindGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "goods.receipt.find")
@Service
public class FindGoodsReceiptService implements FindGoodsReceiptUseCase {
  private final GoodsReceiptRepository repository;

  public FindGoodsReceiptService(GoodsReceiptRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public GoodsReceiptDto findById(Long id, Long companyId) {
    return GoodsReceiptDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new GoodsReceiptNotFoundException(id)));
  }
}
