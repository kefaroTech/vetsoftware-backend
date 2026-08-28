package com.vetsoftware.app.companyusageevent.application.usecase;

import com.vetsoftware.app.companyusageevent.application.command.AttachUsageEventToChargeCommand;
import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.application.port.in.AttachUsageEventToChargeUseCase;
import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEventNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuelga el hecho del cargo que lo facturo.
 *
 * <p>
 * <strong>Carga por la variante acotada por empresa</strong>
 * ({@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}, BE-COV). Con la carga ancha, el
 * cierre de la clinica A podria colgar el hecho de la clinica B de un cargo de
 * A: no seria un rechazo sino una <em>apropiacion</em>, y ademas de las que no
 * se ven, porque el {@code @PreAuthorize} se lee perfecto —solo prueba quien
 * llama, nunca de quien es la fila—. La base pone la segunda barandilla con
 * {@code fk_cue_charge (company_id, charge_id)}, compuesta a proposito, pero la
 * primera es esta linea.
 *
 * <p>
 * Que el hueco estuviera libre lo comprueba el dominio
 * ({@code CompanyUsageEvent#attachToCharge}), no este servicio: es una
 * invariante del hecho y ahi no se puede saltar.
 */
@Observed(name = "company.usage.event.attach.charge")
@Service
public class AttachUsageEventToChargeService implements AttachUsageEventToChargeUseCase {

    private final CompanyUsageEventRepository repository;

    public AttachUsageEventToChargeService(CompanyUsageEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CompanyUsageEventDto execute(AttachUsageEventToChargeCommand command) {
        CompanyUsageEvent event = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new CompanyUsageEventNotFoundException("Company usage event "
                        + command.id() + " not found for company " + command.companyId()));
        return CompanyUsageEventDto.from(repository.save(event.attachToCharge(command.chargeId())));
    }
}
