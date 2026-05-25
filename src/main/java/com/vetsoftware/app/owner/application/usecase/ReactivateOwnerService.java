package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.in.ReactivateOwnerUseCase;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "owner.reactivate")
@Service
public class ReactivateOwnerService implements ReactivateOwnerUseCase {
    private final OwnerRepository repository;

    public ReactivateOwnerService(OwnerRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public OwnerDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new OwnerNotFoundException(id);
        return OwnerDto.from(repository.findById(id)
            .orElseThrow(() -> new OwnerNotFoundException(id)));
    }
}
