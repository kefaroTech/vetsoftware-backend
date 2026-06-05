package com.vetsoftware.app.tax.application.usecase;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.in.ReactivateTaxUseCase;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "tax.reactivate")
@Service
public class ReactivateTaxService implements ReactivateTaxUseCase {
    private final TaxRepository repository;

    public ReactivateTaxService(TaxRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public TaxDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new TaxNotFoundException(id);
        return TaxDto.from(repository.findById(id)
            .orElseThrow(() -> new TaxNotFoundException(id)));
    }
}
