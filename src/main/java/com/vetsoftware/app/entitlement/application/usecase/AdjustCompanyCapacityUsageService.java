package com.vetsoftware.app.entitlement.application.usecase;

import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.port.in.AdjustCompanyCapacityUsageUseCase;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityLimitExceededException;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityNotFoundException;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityUnderflowException;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mueve el consumo con una sola sentencia atomica y despues lee el contador
 * para devolverlo. La lectura va <em>despues</em> a proposito: leer antes para
 * decidir y escribir despues es exactamente la carrera que el
 * {@code UPDATE ... SET x = x
 * + ?} evita.
 *
 * <p>
 * Cero filas afectadas tiene dos causas distintas y se distinguen, porque el
 * mensaje generico "no se pudo actualizar" es lo que convierte un error de
 * configuracion en una hora de depuracion.
 */
@Observed(name = "entitlement.capacity.adjust")
@Service
public class AdjustCompanyCapacityUsageService implements AdjustCompanyCapacityUsageUseCase {

    private final CompanyCapacityRepository repository;

    public AdjustCompanyCapacityUsageService(CompanyCapacityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CompanyCapacityDto execute(AdjustCompanyCapacityUsageCommand command) {
        int updated = repository.addUsage(command.companyId(), command.capacityUnit(),
                command.delta());
        Optional<CompanyCapacity> capacity = repository.findByCompanyIdAndUnit(command.companyId(),
                command.capacityUnit());
        if (updated == 0) {
            CompanyCapacity current = capacity.orElseThrow(() -> notContracted(command));
            long requestedUsage = (long) current.getUsedQuantity() + command.delta();
            if (requestedUsage < 0) {
                throw new CompanyCapacityUnderflowException(command.companyId(),
                        command.capacityUnit(), current.getUsedQuantity(), command.delta());
            }
            if (command.delta() > 0 && requestedUsage > current.getLimitQuantity()) {
                throw new CompanyCapacityLimitExceededException(command.companyId(),
                        command.capacityUnit(), current.getLimitQuantity(),
                        current.getUsedQuantity(), command.delta());
            }
            throw new IllegalStateException("Capacity update affected no rows for company "
                    + command.companyId() + " and unit " + command.capacityUnit().name());
        }
        return CompanyCapacityDto.from(capacity.orElseThrow(() -> notContracted(command)));
    }

    /**
     * Sin fila no hay techo contratado, y eso se lee como <strong>limite
     * cero</strong>, nunca como ilimitado --el razonamiento completo esta en
     * {@link CompanyCapacityNotFoundException}--. El mensaje nombra la unidad y
     * apunta al recalculo porque la causa mas probable no es que el cliente no lo
     * haya contratado, sino que sus contadores todavia no se han derivado.
     */
    private static CompanyCapacityNotFoundException notContracted(
            AdjustCompanyCapacityUsageCommand command) {
        return new CompanyCapacityNotFoundException("Company " + command.companyId()
                + " has no contracted capacity for unit " + command.capacityUnit().name()
                + ": an absent row means limit zero, not unlimited. Recalculate the company"
                + " entitlements if its contract does include this unit");
    }
}
