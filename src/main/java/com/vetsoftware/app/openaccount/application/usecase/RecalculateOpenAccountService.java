package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.port.in.RecalculateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountTotalsPort;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open_account.recalculate")
@Service
public class RecalculateOpenAccountService implements RecalculateOpenAccountUseCase {
    private final OpenAccountRepository repository;
    private final OpenAccountTotalsPort totalsPort;

    public RecalculateOpenAccountService(OpenAccountRepository repository,
                                         OpenAccountTotalsPort totalsPort) {
        this.repository = repository;
        this.totalsPort = totalsPort;
    }

    @Override
    @Transactional
    public void recalculate(Long openAccountId) {
        OpenAccount openAccount = repository.findById(openAccountId)
            .orElseThrow(() -> new OpenAccountNotFoundException(openAccountId));
        BigDecimal total = totalsPort.totalCharges(openAccountId);
        BigDecimal paid = totalsPort.totalPayments(openAccountId);
        openAccount.recalculate(total, paid);
        repository.save(openAccount);
    }
}
