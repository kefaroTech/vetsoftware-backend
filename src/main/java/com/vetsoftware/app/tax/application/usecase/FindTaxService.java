package com.vetsoftware.app.tax.application.usecase;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.in.FindTaxUseCase;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "tax.find")
@Service
public class FindTaxService implements FindTaxUseCase {
    private final TaxRepository repository;

    public FindTaxService(TaxRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaxDto findById(Long id) {
        return TaxDto.from(repository.findById(id)
                .orElseThrow(() -> new TaxNotFoundException(id)));
    }
}
