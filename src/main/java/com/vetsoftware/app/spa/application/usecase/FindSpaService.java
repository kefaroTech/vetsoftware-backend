package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.in.FindSpaUseCase;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "spa.find")
@Service
public class FindSpaService implements FindSpaUseCase {
    private final SpaRepository repository;

    public FindSpaService(SpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SpaDto findById(Long id, Long companyId) {
        return SpaDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new SpaNotFoundException(id)));
    }
}
