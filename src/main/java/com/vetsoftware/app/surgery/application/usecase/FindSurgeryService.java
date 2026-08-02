package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.in.FindSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "surgery.find")
@Service
public class FindSurgeryService implements FindSurgeryUseCase {
    private final SurgeryRepository repository;

    public FindSurgeryService(SurgeryRepository repository) {
        this.repository = repository;
    }

    @Override
    public SurgeryDto findById(Long id, Long companyId) {
        return SurgeryDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new SurgeryNotFoundException(id)));
    }
}
