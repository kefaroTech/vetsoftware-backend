package com.vetsoftware.app.hospitalization.application.usecase;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.in.ListHospitalizationsByCompanyUseCase;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.list.by.company")
@Service
public class ListHospitalizationsByCompanyService implements ListHospitalizationsByCompanyUseCase {
    private final HospitalizationRepository repository;

    public ListHospitalizationsByCompanyService(HospitalizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<HospitalizationDto> listByCompany(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(HospitalizationDto::from)
                .toList();
    }
}
