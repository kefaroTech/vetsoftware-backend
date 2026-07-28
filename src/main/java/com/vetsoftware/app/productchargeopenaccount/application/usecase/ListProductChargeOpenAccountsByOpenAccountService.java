package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ListProductChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "product.charge.open.account.list.by.open.account")
@Service
public class ListProductChargeOpenAccountsByOpenAccountService
        implements ListProductChargeOpenAccountsByOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;

    public ListProductChargeOpenAccountsByOpenAccountService(ProductChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductChargeOpenAccountDto> listByOpenAccount(Long openAccountId, Long companyId) {
        return repository.findByOpenAccountIdAndCompanyId(openAccountId, companyId).stream()
            .map(ProductChargeOpenAccountDto::from).toList();
    }
}
