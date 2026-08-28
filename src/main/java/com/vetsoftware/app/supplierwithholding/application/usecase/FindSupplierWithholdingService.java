package com.vetsoftware.app.supplierwithholding.application.usecase;

import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import com.vetsoftware.app.supplierwithholding.application.port.in.FindSupplierWithholdingUseCase;
import com.vetsoftware.app.supplierwithholding.application.port.out.SupplierWithholdingRepository;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** Una retencion por su id. */
@Observed(name = "supplier.withholding.find")
@Service
public class FindSupplierWithholdingService implements FindSupplierWithholdingUseCase {

    private final SupplierWithholdingRepository repository;

    public FindSupplierWithholdingService(SupplierWithholdingRepository repository) {
        this.repository = repository;
    }

    @Override
    public SupplierWithholdingDto findById(Long id) {
        return repository.findById(id).map(SupplierWithholdingDto::from)
                .orElseThrow(() -> new SupplierWithholdingNotFoundException(id));
    }
}
