package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.in.FindVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination.find")
@Service
public class FindVaccinationService implements FindVaccinationUseCase {
    private final VaccinationRepository repository;

    public FindVaccinationService(VaccinationRepository repository) {
        this.repository = repository;
    }

    @Override
    public VaccinationDto findById(Long id, Long companyId) {
        return VaccinationDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new VaccinationNotFoundException(id)));
    }
}
