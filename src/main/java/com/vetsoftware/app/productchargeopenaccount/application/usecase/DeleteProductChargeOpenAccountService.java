package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import com.vetsoftware.app.productchargeopenaccount.application.port.in.DeleteProductChargeOpenAccountUseCase;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountTotalsQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "product_charge_open_account.delete")
@Service
public class DeleteProductChargeOpenAccountService implements DeleteProductChargeOpenAccountUseCase {
    private final ProductChargeOpenAccountRepository repository;
    private final OpenAccountTotalsQueryPort openAccountTotals;
    private final OpenAccountRefresher refresher;

    public DeleteProductChargeOpenAccountService(ProductChargeOpenAccountRepository repository,
                                                 OpenAccountTotalsQueryPort openAccountTotals,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.openAccountTotals = openAccountTotals;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        ProductChargeOpenAccount charge = repository.findById(id)
            .orElseThrow(() -> new ProductChargeOpenAccountNotFoundException(id));
        if (!charge.getOpenAccount().companyId().equals(companyId)) {
            throw new IllegalArgumentException("product charge does not belong to company");
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
