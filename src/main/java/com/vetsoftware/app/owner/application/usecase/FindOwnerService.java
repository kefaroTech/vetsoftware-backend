package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.in.FindOwnerUseCase;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "owner.find")
@Service
public class FindOwnerService implements FindOwnerUseCase {
    private final OwnerRepository repository;

    public FindOwnerService(OwnerRepository repository) {
        this.repository = repository;
    }

    @Override
    public OwnerDto findById(Long id, Long companyId) {
        return OwnerDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new OwnerNotFoundException(id)));
    }
}
