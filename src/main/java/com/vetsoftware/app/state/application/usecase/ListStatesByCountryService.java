package com.vetsoftware.app.state.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.in.ListStatesByCountryUseCase;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "state.listByCountry")
@Service
public class ListStatesByCountryService implements ListStatesByCountryUseCase {
    private final StateRepository repository;

    public ListStatesByCountryService(StateRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StateDto> listByCountry(Long countryId, AuthContext auth) {
        return repository.findByCountryId(countryId).stream().map(StateDto::from).toList();
    }
}
