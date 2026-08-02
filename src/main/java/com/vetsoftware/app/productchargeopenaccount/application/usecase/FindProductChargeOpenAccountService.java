package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.FindProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "product.charge.open.account.find")
@Service
public class FindProductChargeOpenAccountService implements FindProductChargeOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;

    public FindProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductChargeOpenAccountDto findById(Long id, Long companyId) {
        return ProductChargeOpenAccountDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ProductChargeOpenAccountNotFoundException(id)));
    }
}
