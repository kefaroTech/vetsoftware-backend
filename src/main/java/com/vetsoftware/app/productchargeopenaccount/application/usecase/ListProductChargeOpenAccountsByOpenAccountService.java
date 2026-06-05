package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ListProductChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "product_charge_open_account.list_by_open_account")
@Service
public class ListProductChargeOpenAccountsByOpenAccountService
        implements ListProductChargeOpenAccountsByOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;

    public ListProductChargeOpenAccountsByOpenAccountService(ProductChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductChargeOpenAccountDto> listByOpenAccount(Long openAccountId, Long companyId) {
        return repository.findByOpenAccountId(openAccountId).stream()
            .map(ProductChargeOpenAccountDto::from).toList();
    }
}
