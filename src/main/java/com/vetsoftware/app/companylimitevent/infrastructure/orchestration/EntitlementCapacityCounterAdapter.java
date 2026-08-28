package com.vetsoftware.app.companylimitevent.infrastructure.orchestration;

import com.vetsoftware.app.companylimitevent.application.port.out.CapacityCounterPort;
import com.vetsoftware.app.entitlement.application.command.MarkCapacityUsageReconciledCommand;
import com.vetsoftware.app.entitlement.application.port.in.ListUnreconciledCapacityCountersUseCase;
import com.vetsoftware.app.entitlement.application.port.in.MarkCapacityUsageReconciledUseCase;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Conecta el recuento con el contador, que vive en otra rodaja.
 *
 * <p>
 * Esta en {@code infrastructure/orchestration} y no en {@code application}
 * porque es el unico sitio de esta feature autorizado a conocer la otra, igual
 * que {@code EntitlementCompanyUsageAdjustmentAdapter}. La dependencia va
 * siempre en el mismo sentido --{@code companylimitevent} conoce a
 * {@code entitlement}, nunca al reves--, que es lo que impide que las dos
 * rodajas acaben mirandose entre si.
 *
 * <p>
 * Traduce el DTO de la otra rodaja al {@code record} propio: importar
 * {@code CompanyCapacityDto} en el {@code application} de esta feature seria
 * romper el vertical slicing por comodidad, y ademas ataria el recuento a un
 * tipo que cambia por motivos que no son suyos.
 */
@Component
public class EntitlementCapacityCounterAdapter implements CapacityCounterPort {

    private final ListUnreconciledCapacityCountersUseCase listUnreconciled;
    private final MarkCapacityUsageReconciledUseCase markReconciled;

    public EntitlementCapacityCounterAdapter(
            ListUnreconciledCapacityCountersUseCase listUnreconciled,
            MarkCapacityUsageReconciledUseCase markReconciled) {
        this.listUnreconciled = listUnreconciled;
        this.markReconciled = markReconciled;
    }

    @Override
    public List<CapacityCounter> findUnreconciled(LocalDateTime staleBefore, long afterId,
            int limit) {
        return listUnreconciled.list(staleBefore, afterId, limit).stream()
                .map(dto -> new CapacityCounter(dto.id(), dto.companyId(), dto.limitDimensionId(),
                        dto.dimensionCode(), dto.measureKind(), dto.periodKey(),
                        dto.limitQuantity(), dto.usedQuantity()))
                .toList();
    }

    @Override
    public boolean markReconciled(Long companyId, Long limitDimensionId, String periodKey,
            LocalDateTime reconciledAt) {
        return markReconciled.execute(new MarkCapacityUsageReconciledCommand(companyId,
                limitDimensionId, periodKey, reconciledAt));
    }
}
