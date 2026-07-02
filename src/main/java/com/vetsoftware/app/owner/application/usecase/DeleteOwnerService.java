package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.owner.application.port.in.DeleteOwnerUseCase;
import com.vetsoftware.app.owner.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.OwnerHasActiveChildrenException;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "owner.delete")
@Service
public class DeleteOwnerService implements DeleteOwnerUseCase {
    private final OwnerRepository repository;
    private final AnimalChildrenQueryPort animalChildrenQueryPort;

    public DeleteOwnerService(
            OwnerRepository repository,
            AnimalChildrenQueryPort animalChildrenQueryPort) {
        this.repository = repository;
        this.animalChildrenQueryPort = animalChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId).orElseThrow(() -> new OwnerNotFoundException(id));
        if (animalChildrenQueryPort.existsActiveByOwnerId(id)) {
            throw new OwnerHasActiveChildrenException(id, "animal");
        }
        repository.delete(id, companyId);
    }
}
