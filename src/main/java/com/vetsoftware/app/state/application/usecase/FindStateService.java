package com.vetsoftware.app.state.application.usecase;

import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.in.FindStateUseCase;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.StateNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "state.find")
@Service
public class FindStateService implements FindStateUseCase {
    private final StateRepository repository;

    public FindStateService(StateRepository repository) {
        this.repository = repository;
    }

    @Override
    public StateDto findById(Long id) {
        return repository.findById(id).map(StateDto::from)
                .orElseThrow(() -> new StateNotFoundException(id));
    }
}
