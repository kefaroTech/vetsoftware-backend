package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.port.in.DeleteGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountTotalsQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "general_charge_open_account.delete")
@Service
public class DeleteGeneralChargeOpenAccountService implements DeleteGeneralChargeOpenAccountUseCase {
    private final GeneralChargeOpenAccountRepository repository;
    private final OpenAccountTotalsQueryPort openAccountTotals;
    private final OpenAccountRefresher refresher;

    public DeleteGeneralChargeOpenAccountService(GeneralChargeOpenAccountRepository repository,
                                                 OpenAccountTotalsQueryPort openAccountTotals,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.openAccountTotals = openAccountTotals;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        GeneralChargeOpenAccount charge = repository.findById(id)
            .orElseThrow(() -> new GeneralChargeOpenAccountNotFoundException(id));
        if (!charge.getOpenAccount().companyId().equals(companyId)) {
            throw new IllegalArgumentException("general charge does not belong to company");
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
