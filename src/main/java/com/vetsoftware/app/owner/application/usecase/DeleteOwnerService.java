package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.owner.application.port.in.DeleteOwnerUseCase;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "owner.delete")
@Service
public class DeleteOwnerService implements DeleteOwnerUseCase {
    private final OwnerRepository repository;

    public DeleteOwnerService(OwnerRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new OwnerNotFoundException(id));
        repository.delete(id);
    }
}
