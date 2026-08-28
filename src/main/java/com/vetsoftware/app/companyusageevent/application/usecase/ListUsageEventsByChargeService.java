package com.vetsoftware.app.companyusageevent.application.usecase;

import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.companyusageevent.application.port.in.ListUsageEventsByChargeUseCase;
import com.vetsoftware.app.companyusageevent.application.port.out.CompanyUsageEventRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El desglose de un cargo por excedente, hecho a hecho: la consulta por la que
 * esta tabla existe.
 *
 * <p>
 * Acota por empresa <em>y</em> por cargo, en ese orden, que es el de
 * {@code ix_cue_charge (company_id, charge_id)}. Sin la empresa delante el
 * indice no sirve y el desglose de un cargo podria arrastrar hechos de otra
 * clinica.
 */
@Observed(name = "company.usage.event.list.by.charge")
@Service
public class ListUsageEventsByChargeService implements ListUsageEventsByChargeUseCase {

    private final CompanyUsageEventRepository repository;

    public ListUsageEventsByChargeService(CompanyUsageEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CompanyUsageEventDto> listByCharge(Long companyId, Long chargeId, int page,
            int pageSize) {
        return repository.findAllByCompanyIdAndChargeId(companyId, chargeId, page, pageSize)
                .map(CompanyUsageEventDto::from);
    }
}
