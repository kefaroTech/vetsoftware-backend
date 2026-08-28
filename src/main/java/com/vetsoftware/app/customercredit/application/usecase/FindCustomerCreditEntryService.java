package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.in.FindCustomerCreditEntryUseCase;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "customer.credit.entry.find")
@Service
public class FindCustomerCreditEntryService implements FindCustomerCreditEntryUseCase {

    private final CustomerCreditEntryRepository repository;

    public FindCustomerCreditEntryService(CustomerCreditEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public CustomerCreditEntryDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(CustomerCreditEntryDto::from)
                .orElseThrow(() -> new CustomerCreditEntryNotFoundException(id));
    }
}
