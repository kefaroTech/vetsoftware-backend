package com.vetsoftware.app.entitlement.application.usecase;

import com.vetsoftware.app.entitlement.application.dto.CompanyEntitlementDto;
import com.vetsoftware.app.entitlement.application.port.in.ListCompanyEntitlementsUseCase;
import com.vetsoftware.app.entitlement.application.port.out.CompanyEntitlementRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** Listado de auditoria de los permisos de una empresa, paginado. */
@Observed(name = "entitlement.list")
@Service
public class ListCompanyEntitlementsService implements ListCompanyEntitlementsUseCase {

    private final CompanyEntitlementRepository repository;

    public ListCompanyEntitlementsService(CompanyEntitlementRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<CompanyEntitlementDto> listByCompanyId(Long companyId, int page,
            int pageSize) {
        return repository.findPageByCompanyId(companyId, page, pageSize)
                .map(CompanyEntitlementDto::from);
    }
}
