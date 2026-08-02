package com.vetsoftware.app.state.application.usecase;

import com.vetsoftware.app.state.application.port.in.DeleteStateUseCase;
import com.vetsoftware.app.state.application.port.out.CityChildrenQueryPort;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.StateHasActiveChildrenException;
import com.vetsoftware.app.state.domain.StateNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "state.delete")
@Service
public class DeleteStateService implements DeleteStateUseCase {
    private final StateRepository repository;
    private final CityChildrenQueryPort cityChildrenQueryPort;

    public DeleteStateService(StateRepository repository,
            CityChildrenQueryPort cityChildrenQueryPort) {
        this.repository = repository;
        this.cityChildrenQueryPort = cityChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new StateNotFoundException(id));
        if (cityChildrenQueryPort.existsActiveByStateId(id)) {
            throw new StateHasActiveChildrenException(id, "city");
        }
        repository.delete(id);
    }
}
