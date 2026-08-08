package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.dto.PageResult;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ListProductChargeOpenAccountsUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "product.charge.open.account.list.all")
@Service
public class ListProductChargeOpenAccountsService implements ListProductChargeOpenAccountsUseCase {
    private final ProductChargeOpenAccountRepository repository;

    public ListProductChargeOpenAccountsService(ProductChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ProductChargeOpenAccountDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(ProductChargeOpenAccountDto::from);
    }
}
