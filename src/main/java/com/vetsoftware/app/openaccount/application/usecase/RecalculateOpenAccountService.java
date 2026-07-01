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
    public void recalculate(Long companyId, Long openAccountId) {
        // Bloqueo pesimista scoped a la empresa: serializa recálculos concurrentes (cargos/abonos simultáneos)
        // sobre la misma cuenta, y solo toma el lock si la cuenta pertenece a companyId (una cuenta ajena
        // lanza NotFound sin bloquear su fila).
        OpenAccount openAccount = repository.findByIdForUpdateAndCompanyId(openAccountId, companyId)
            .orElseThrow(() -> new OpenAccountNotFoundException(openAccountId));
        BigDecimal total = totalsPort.totalCharges(openAccountId);
        BigDecimal paid = totalsPort.totalPayments(openAccountId);
        openAccount.recalculate(total, paid);
        repository.save(openAccount);
    }
}
