package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.port.in.DeleteProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product_charge_open_account.delete")
@Service
public class DeleteProductChargeOpenAccountService implements DeleteProductChargeOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;
    private final OpenAccountRefresher refresher;

    public DeleteProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        ProductChargeOpenAccount charge = repository.findById(id)
            .orElseThrow(() -> new ProductChargeOpenAccountNotFoundException(id));
        Long openAccountId = charge.getOpenAccount().id();
        repository.delete(id);
        refresher.refresh(openAccountId);
    }
}
