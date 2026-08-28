package com.vetsoftware.app.entitlement.application.usecase;

import com.vetsoftware.app.entitlement.application.command.MarkCapacityUsageReconciledCommand;
import com.vetsoftware.app.entitlement.application.port.in.MarkCapacityUsageReconciledUseCase;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escribe el sello del consumo, que hasta hoy no lo escribia nadie.
 *
 * <p>
 * {@code usage_reconciled_at} existe desde el changeset 314 con su indice
 * {@code ix_company_capacities_unreconciled}, y su valor iba a ser {@code null}
 * para siempre: el recalculo no lo toca a proposito --no mira el consumo-- y no
 * habia ningun otro escritor. Una columna correcta que nadie escribe no es
 * media garantia, es ninguna.
 */
@Service
public class MarkCapacityUsageReconciledService implements MarkCapacityUsageReconciledUseCase {

    private final CompanyCapacityRepository repository;

    public MarkCapacityUsageReconciledService(CompanyCapacityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean execute(MarkCapacityUsageReconciledCommand command) {
        return repository.markUsageReconciled(command.companyId(), command.limitDimensionId(),
                command.periodKey(), command.reconciledAt()) > 0;
    }
}
