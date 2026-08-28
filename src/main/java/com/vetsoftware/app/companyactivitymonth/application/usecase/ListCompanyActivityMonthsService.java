package com.vetsoftware.app.companyactivitymonth.application.usecase;

import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.application.port.in.ListCompanyActivityMonthsUseCase;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyActivityMonthRepository;
import com.vetsoftware.app.companyactivitymonth.domain.ActivityPeriodKey;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los tres listados de la serie de actividad: toda, la de una clinica y la de
 * un mes.
 *
 * <p>
 * <strong>Los totales son los de la consulta, no los del contenido ya
 * paginado.</strong> {@code PageResult} viene armado desde el adaptador con el
 * total real; recontarlo aqui sobre los veinte elementos de la pagina daria un
 * numero que parece correcto y no lo es.
 */
@Observed(name = "companyactivitymonth.list")
@Service
public class ListCompanyActivityMonthsService implements ListCompanyActivityMonthsUseCase {

    private final CompanyActivityMonthRepository repository;

    public ListCompanyActivityMonthsService(CompanyActivityMonthRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CompanyActivityMonthDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(CompanyActivityMonthDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CompanyActivityMonthDto> listByCompany(Long companyId, int page,
            int pageSize) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(CompanyActivityMonthDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CompanyActivityMonthDto> listByPeriod(String periodKey, int page,
            int pageSize) {
        return repository
                .findAllByPeriodKey(new ActivityPeriodKey(periodKey).value(), page, pageSize)
                .map(CompanyActivityMonthDto::from);
    }
}
