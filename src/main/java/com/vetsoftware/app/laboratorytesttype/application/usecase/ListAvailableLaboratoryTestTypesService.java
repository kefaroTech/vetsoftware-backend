package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.ListAvailableLaboratoryTestTypesUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.type.list.available")
@Service
public class ListAvailableLaboratoryTestTypesService implements ListAvailableLaboratoryTestTypesUseCase {
    private final LaboratoryTestTypeRepository repository;

    public ListAvailableLaboratoryTestTypesService(LaboratoryTestTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<LaboratoryTestTypeDto> listAvailable(Long companyId) {
        return repository.findAllAvailableForCompany(companyId).stream().map(LaboratoryTestTypeDto::from).toList();
    }
}
