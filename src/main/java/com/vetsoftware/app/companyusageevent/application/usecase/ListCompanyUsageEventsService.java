package com.vetsoftware.app.companyusageevent.application.usecase;

import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.application.port.in.ListCompanyUsageEventsUseCase;
import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los dos listados de hechos: el barrido de plataforma y el de una clinica.
 *
 * <p>
 * Los totales son los de la consulta y no se recalculan sobre el contenido ya
 * paginado: {@code PageResult#map} los conserva intactos. Sobre una tabla cuya
 * proyeccion son doce millones de filas, recalcularlos seria ademas contar la
 * tabla entera en cada pagina.
 */
@Observed(name = "company.usage.event.list")
@Service
public class ListCompanyUsageEventsService implements ListCompanyUsageEventsUseCase {

    private final CompanyUsageEventRepository repository;

    public ListCompanyUsageEventsService(CompanyUsageEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CompanyUsageEventDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(CompanyUsageEventDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CompanyUsageEventDto> listByCompany(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(CompanyUsageEventDto::from);
    }
}
