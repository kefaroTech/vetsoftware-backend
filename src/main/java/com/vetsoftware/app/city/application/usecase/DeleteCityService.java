package com.vetsoftware.app.city.application.usecase;

import com.vetsoftware.app.city.application.port.in.DeleteCityUseCase;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.application.port.out.OwnerChildrenQueryPort;
import com.vetsoftware.app.city.domain.CityHasActiveChildrenException;
import com.vetsoftware.app.city.domain.CityNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "city.delete")
@Service
public class DeleteCityService implements DeleteCityUseCase {
    private final CityRepository repository;
    private final OwnerChildrenQueryPort ownerChildrenQueryPort;

    public DeleteCityService(CityRepository repository,
            OwnerChildrenQueryPort ownerChildrenQueryPort) {
        this.repository = repository;
        this.ownerChildrenQueryPort = ownerChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new CityNotFoundException(id));
        if (ownerChildrenQueryPort.existsActiveByCityId(id)) {
            throw new CityHasActiveChildrenException(id, "owner");
        }
        repository.delete(id);
    }
}
