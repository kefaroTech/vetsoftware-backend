package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.port.in.DeleteServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountTotalsQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service_charge_open_account.delete")
@Service
public class DeleteServiceChargeOpenAccountService implements DeleteServiceChargeOpenAccountUseCase {
    private final ServiceChargeOpenAccountRepository repository;
    private final OpenAccountTotalsQueryPort openAccountTotals;
    private final OpenAccountRefresher refresher;

    public DeleteServiceChargeOpenAccountService(ServiceChargeOpenAccountRepository repository,
                                                 OpenAccountTotalsQueryPort openAccountTotals,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.openAccountTotals = openAccountTotals;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        ServiceChargeOpenAccount charge = repository.findById(id)
            .orElseThrow(() -> new ServiceChargeOpenAccountNotFoundException(id));
        if (!charge.getOpenAccount().companyId().equals(companyId)) {
            throw new IllegalArgumentException("service charge does not belong to company");
        }
        Long openAccountId = charge.getOpenAccount().id();

        // Soft-delete (enabled=false). Hibernate hace flush antes de los SUM de abajo,
        // así que totalCharges ya refleja la baja de este cargo.
        repository.delete(id);

        // Un cargo no puede eliminarse si los abonos registrados pasarían a superar el total
        // de cargos restante (saldo negativo): primero hay que anular los abonos. Lanzar aquí
        // revierte el soft-delete por el rollback de la transacción.
        BigDecimal remainingCharges = openAccountTotals.totalCharges(openAccountId);
        BigDecimal payments = openAccountTotals.totalPayments(openAccountId);
        if (payments.compareTo(remainingCharges) > 0) {
            throw new IllegalStateException(
                "No puedes eliminar este cargo: la cuenta tiene abonos que lo cubren. "
                    + "Anula primero los abonos.");
        }

        refresher.refresh(openAccountId);
    }
}
