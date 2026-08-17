package com.vetsoftware.app.supplier.application.usecase;

import com.vetsoftware.app.supplier.application.command.SearchSuppliersCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.in.SearchSuppliersUseCase;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "supplier.search")
@Service
public class SearchSuppliersService implements SearchSuppliersUseCase {
    private final SupplierRepository repository;

    public SearchSuppliersService(SupplierRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SupplierDto> execute(SearchSuppliersCommand command) {
        return repository.search(command).map(SupplierDto::from);
    }
}
