package com.vetsoftware.app.companyusageevent.application.usecase;

import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.application.port.in.FindCompanyUsageEventUseCase;
import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEventNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Un hecho concreto, para el expediente de una reclamacion.
 *
 * <p>
 * <strong>Usa la carga ancha a proposito, y solo puede porque el puerto de
 * entrada esta cerrado a {@code hasRole('SYSTEM')} a secas.</strong> Un
 * principal de plataforma no tiene empresa contra la que acotar, que es la
 * exencion literal que contempla {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}. Si
 * algun dia esta operacion se abre a un tenant, hay que cambiar la carga
 * <b>antes</b> que la anotacion: al reves, el gate se lee bien y la fuga queda
 * debajo.
 */
@Observed(name = "company.usage.event.find")
@Service
public class FindCompanyUsageEventService implements FindCompanyUsageEventUseCase {

    private final CompanyUsageEventRepository repository;

    public FindCompanyUsageEventService(CompanyUsageEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyUsageEventDto findById(Long id) {
        return repository.findById(id).map(CompanyUsageEventDto::from)
                .orElseThrow(() -> new CompanyUsageEventNotFoundException(id));
    }
}
