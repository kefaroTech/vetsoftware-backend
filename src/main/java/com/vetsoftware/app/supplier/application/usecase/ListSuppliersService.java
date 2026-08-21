package com.vetsoftware.app.supplier.application.usecase;

import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.in.ListSuppliersUseCase;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "supplier.list.by.company")
@Service
public class ListSuppliersService implements ListSuppliersUseCase {
    private final SupplierRepository repository;

    public ListSuppliersService(SupplierRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SupplierDto> listByCompany(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(SupplierDto::from).toList();
    }
}
