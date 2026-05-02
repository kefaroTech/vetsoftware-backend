package com.vetsoftware.app.state.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.in.ListStatesUseCase;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "state.list")
@Service
public class ListStatesService implements ListStatesUseCase {
    private final StateRepository repository;

    public ListStatesService(StateRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StateDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(StateDto::from).toList();
    }
}
