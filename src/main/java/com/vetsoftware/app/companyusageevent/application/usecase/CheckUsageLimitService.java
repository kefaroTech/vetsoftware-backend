package com.vetsoftware.app.companyusageevent.application.usecase;

import com.vetsoftware.app.companyusageevent.application.command.CheckUsageLimitCommand;
import com.vetsoftware.app.companyusageevent.application.port.in.CheckUsageLimitUseCase;
import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.companyusageevent.application.port.out.EffectiveUsageLimitPort;
import com.vetsoftware.app.companyusageevent.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.companyusageevent.application.port.out.UsageLimitDenialPort;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageLimitExceededException;
import com.vetsoftware.app.companyusageevent.domain.EffectiveUsageLimit;
import com.vetsoftware.app.companyusageevent.domain.LimitDimensionRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Techos gratuitos en runtime para los ejes {@code CUMULATIVE}/{@code FLOW}
 * (mascotas, propietarios, citas, servicios de spa/guardería).
 *
 * <p>
 * <strong>No hay un {@code UPDATE ... SET used = used + 1} atómico que proteja
 * esto</strong>, al contrario que {@code company_capacities}: aquí el consumo
 * es un {@code COUNT(*)} sobre la bitácora de hechos, así que dos altas
 * concurrentes en el mismo instante pueden las dos ver margen y las dos crear.
 * Es una ventana de carrera aceptada a propósito: solo se bloquea crear por
 * encima del techo, no se garantiza exactitud bajo concurrencia — el mismo
 * trade-off que {@code uq_appointments_active_employee_start}.
 */
@Observed(name = "company.usage.event.check.limit")
@Service
public class CheckUsageLimitService implements CheckUsageLimitUseCase {

    private final LimitDimensionQueryPort limitDimensionQueryPort;
    private final CompanyUsageEventRepository repository;
    private final EffectiveUsageLimitPort effectiveUsageLimitPort;
    private final UsageLimitDenialPort limitDenialPort;

    public CheckUsageLimitService(LimitDimensionQueryPort limitDimensionQueryPort,
            CompanyUsageEventRepository repository, EffectiveUsageLimitPort effectiveUsageLimitPort,
            UsageLimitDenialPort limitDenialPort) {
        this.limitDimensionQueryPort = limitDimensionQueryPort;
        this.repository = repository;
        this.effectiveUsageLimitPort = effectiveUsageLimitPort;
        this.limitDenialPort = limitDenialPort;
    }

    @Override
    @Transactional(readOnly = true)
    public void execute(CheckUsageLimitCommand command) {
        LimitDimensionRef dimension = limitDimensionQueryPort
                .findByCode(command.limitDimensionCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown limit dimension code: " + command.limitDimensionCode()));
        EffectiveUsageLimit limit = effectiveUsageLimitPort.resolve(command.companyId(),
                dimension.id());
        if (limit.unlimited()) {
            return;
        }
        long used = repository.countCurrent(command.companyId(), dimension.id(),
                command.periodKey());
        if (used + 1 > limit.limitQuantity()) {
            limitDenialPort.limitDenied(command.companyId(), dimension.id(), limit.limitQuantity(),
                    (int) used, 1);
            throw new CompanyUsageLimitExceededException(command.companyId(),
                    command.limitDimensionCode(), limit.limitQuantity(), (int) used, 1);
        }
    }
}
