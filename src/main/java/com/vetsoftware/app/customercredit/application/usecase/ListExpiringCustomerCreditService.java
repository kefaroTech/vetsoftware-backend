package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.in.ListExpiringCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** Barrido de plataforma: saldos que caducan, de todas las clinicas. */
@Observed(name = "customer.credit.list.expiring")
@Service
public class ListExpiringCustomerCreditService implements ListExpiringCustomerCreditUseCase {

    private final CustomerCreditEntryRepository repository;

    public ListExpiringCustomerCreditService(CustomerCreditEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<CustomerCreditEntryDto> listExpiring(LocalDate before, int page,
            int pageSize) {
        return repository.findAllExpiringBefore(before, page, pageSize)
                .map(CustomerCreditEntryDto::from);
    }
}
