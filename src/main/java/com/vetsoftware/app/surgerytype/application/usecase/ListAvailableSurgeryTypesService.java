package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.ListAvailableSurgeryTypesUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "surgery.type.list.available")
@Service
public class ListAvailableSurgeryTypesService implements ListAvailableSurgeryTypesUseCase {
    private final SurgeryTypeRepository repository;

    public ListAvailableSurgeryTypesService(SurgeryTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SurgeryTypeDto> listAvailable(Long companyId) {
        return repository.findAllAvailableForCompany(companyId).stream().map(SurgeryTypeDto::from)
                .toList();
    }
}
