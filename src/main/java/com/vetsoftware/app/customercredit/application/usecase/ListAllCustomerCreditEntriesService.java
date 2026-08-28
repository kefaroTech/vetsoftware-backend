package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.in.ListAllCustomerCreditEntriesUseCase;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "customer.credit.entry.list.all")
@Service
public class ListAllCustomerCreditEntriesService implements ListAllCustomerCreditEntriesUseCase {

    private final CustomerCreditEntryRepository repository;

    public ListAllCustomerCreditEntriesService(CustomerCreditEntryRepository repository) {
        this.repository = repository;
    }

    /**
     * {@code companyId} es un filtro opcional de la consola, no un control de
     * acceso: quien llega aqui ya es SYSTEM. Sin el, el listado es cross-tenant a
     * proposito.
     */
    @Override
    public PageResult<CustomerCreditEntryDto> listAll(Long companyId, int page, int pageSize) {
        PageResult<CustomerCreditEntry> found = companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize);
        return found.map(CustomerCreditEntryDto::from);
    }
}
