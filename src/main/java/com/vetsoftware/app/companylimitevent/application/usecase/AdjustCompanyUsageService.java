package com.vetsoftware.app.companylimitevent.application.usecase;

import com.vetsoftware.app.companylimitevent.application.command.AdjustCompanyUsageCommand;
import com.vetsoftware.app.companylimitevent.application.dto.CompanyLimitEventDto;
import com.vetsoftware.app.companylimitevent.application.port.in.AdjustCompanyUsageUseCase;
import com.vetsoftware.app.companylimitevent.application.port.out.CompanyLimitEventRepository;
import com.vetsoftware.app.companylimitevent.application.port.out.CompanyUsageAdjustmentPort;
import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrige el consumo de un contador dejando el hecho que lo compensa.
 *
 * <p>
 * Lee los dos números <em>antes</em> de mover el contador y los copia en el
 * hecho: es lo que hace que dentro de un año se pueda reconstruir de qué cifra
 * se partía. Después mueve el contador con la instrucción atómica que ya existe
 * —esta feature no reimplementa el conteo— y escribe el hecho con la firma de
 * quien lo hizo y su motivo obligatorio.
 *
 * <p>
 * El hecho va en la <em>misma</em> transacción que la corrección, y aquí eso es
 * lo correcto: si el movimiento del contador falla, no hubo corrección que
 * registrar. Es el caso opuesto al del portazo, donde el hecho tiene que
 * sobrevivir precisamente porque la operación se deshace.
 */
@Service
public class AdjustCompanyUsageService implements AdjustCompanyUsageUseCase {

    private final CompanyLimitEventRepository repository;
    private final CompanyUsageAdjustmentPort usageAdjustmentPort;
    private final Clock clock;

    public AdjustCompanyUsageService(CompanyLimitEventRepository repository,
            CompanyUsageAdjustmentPort usageAdjustmentPort, Clock clock) {
        this.repository = repository;
        this.usageAdjustmentPort = usageAdjustmentPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyLimitEventDto execute(AdjustCompanyUsageCommand command) {
        CompanyUsageAdjustmentPort.UsageSnapshot before = usageAdjustmentPort
                .currentUsage(command.companyId(), command.capacityUnit());
        usageAdjustmentPort.adjustUsage(command.companyId(), command.capacityUnit(),
                command.delta());
        CompanyLimitEvent event = CompanyLimitEvent.record(command.companyId(),
                command.limitDimensionId(), LimitEventType.USAGE_ADJUSTED, before.limitQuantity(),
                before.usedQuantity(), command.delta(), LimitSource.NONE, null,
                EventActor.systemUser(command.systemUserId()), command.reasonCode(),
                command.reason(), LocalDateTime.now(clock));
        return CompanyLimitEventDto.from(repository.append(event));
    }
}
