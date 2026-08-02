package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ListProductChargeOpenAccountsUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "product.charge.open.account.list.all")
@Service
public class ListProductChargeOpenAccountsService implements ListProductChargeOpenAccountsUseCase {
    private final ProductChargeOpenAccountRepository repository;

    public ListProductChargeOpenAccountsService(ProductChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductChargeOpenAccountDto> listAll(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream()
                .map(ProductChargeOpenAccountDto::from).toList();
    }
}
