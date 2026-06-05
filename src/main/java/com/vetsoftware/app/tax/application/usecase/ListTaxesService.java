package com.vetsoftware.app.tax.application.usecase;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.in.ListTaxesUseCase;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "tax.list")
@Service
public class ListTaxesService implements ListTaxesUseCase {
    private final TaxRepository repository;

    public ListTaxesService(TaxRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TaxDto> listByCompany(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(TaxDto::from).toList();
    }
}
