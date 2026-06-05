package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ListProductChargeOpenAccountsUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "product_charge_open_account.list_all")
@Service
public class ListProductChargeOpenAccountsService implements ListProductChargeOpenAccountsUseCase {
    private final ProductChargeOpenAccountRepository repository;

    public ListProductChargeOpenAccountsService(ProductChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductChargeOpenAccountDto> listAll() {
        return repository.findAll().stream().map(ProductChargeOpenAccountDto::from).toList();
    }
}
