package com.vetsoftware.app.state.application.usecase;

import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.in.ReactivateStateUseCase;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.StateNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "state.reactivate")
@Service
public class ReactivateStateService implements ReactivateStateUseCase {
    private final StateRepository repository;

    public ReactivateStateService(StateRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public StateDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new StateNotFoundException(id);
        return StateDto.from(repository.findById(id).orElseThrow(() -> new StateNotFoundException(id)));
    }
}
