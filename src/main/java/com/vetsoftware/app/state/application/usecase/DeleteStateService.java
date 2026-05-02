package com.vetsoftware.app.state.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.state.application.port.in.DeleteStateUseCase;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.StateNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "state.delete")
@Service
public class DeleteStateService implements DeleteStateUseCase {
    private final StateRepository repository;

    public DeleteStateService(StateRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new StateNotFoundException(id));
        repository.delete(id);
    }
}
