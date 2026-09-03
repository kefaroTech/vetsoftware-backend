package com.vetsoftware.app.taxreturn.application.usecase;

import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import com.vetsoftware.app.taxreturn.application.port.in.FindTaxReturnUseCase;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturnNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "tax.return.find")
@Service
public class FindTaxReturnService implements FindTaxReturnUseCase {

    private final TaxReturnRepository repository;

    public FindTaxReturnService(TaxReturnRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaxReturnDto findById(Long id) {
        return repository.findById(id).map(TaxReturnDto::from)
                .orElseThrow(() -> new TaxReturnNotFoundException(id));
    }
}
