package com.vetsoftware.app.country.application.usecase;

import com.vetsoftware.app.country.application.port.in.DeleteCountryUseCase;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.application.port.out.StateChildrenQueryPort;
import com.vetsoftware.app.country.domain.CountryHasActiveChildrenException;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "country.delete")
@Service
public class DeleteCountryService implements DeleteCountryUseCase {
    private final CountryRepository repository;
    private final StateChildrenQueryPort stateChildrenQueryPort;

    public DeleteCountryService(
            CountryRepository repository,
            StateChildrenQueryPort stateChildrenQueryPort) {
        this.repository = repository;
        this.stateChildrenQueryPort = stateChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new CountryNotFoundException(id));
        if (stateChildrenQueryPort.existsActiveByCountryId(id)) {
            throw new CountryHasActiveChildrenException(id, "state");
        }
        repository.delete(id);
    }
}
