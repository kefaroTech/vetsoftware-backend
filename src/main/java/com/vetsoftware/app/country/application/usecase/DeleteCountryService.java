package com.vetsoftware.app.country.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.country.application.port.in.DeleteCountryUseCase;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "country.delete")
@Service
public class DeleteCountryService implements DeleteCountryUseCase {
    private final CountryRepository repository;

    public DeleteCountryService(CountryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new CountryNotFoundException(id));
        repository.delete(id);
    }
}
