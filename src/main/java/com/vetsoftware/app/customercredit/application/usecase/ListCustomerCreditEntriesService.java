package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.in.ListCustomerCreditEntriesUseCase;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "customer.credit.entry.list.by.company")
@Service
public class ListCustomerCreditEntriesService implements ListCustomerCreditEntriesUseCase {

    private final CustomerCreditEntryRepository repository;

    public ListCustomerCreditEntriesService(CustomerCreditEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<CustomerCreditEntryDto> listByCompany(Long companyId, int page,
            int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize)
                .map(CustomerCreditEntryDto::from);
    }
}
