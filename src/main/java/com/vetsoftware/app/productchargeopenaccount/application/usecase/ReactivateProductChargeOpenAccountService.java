package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.in.ReactivateProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product_charge_open_account.reactivate")
@Service
public class ReactivateProductChargeOpenAccountService implements ReactivateProductChargeOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;
    private final OpenAccountRefresher refresher;

    public ReactivateProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
                                                     OpenAccountRefresher refresher) {
        this.repository = repository;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public ProductChargeOpenAccountDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0) throw new ProductChargeOpenAccountNotFoundException(id);
        ProductChargeOpenAccount charge = repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ProductChargeOpenAccountNotFoundException(id));
        refresher.refresh(companyId, charge.getOpenAccount().id());
        return ProductChargeOpenAccountDto.from(charge);
    }
}
