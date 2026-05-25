package com.vetsoftware.app.state.application.port.out;

import com.vetsoftware.app.state.domain.State;
import java.util.List;
import java.util.Optional;

public interface StateRepository {
    State save(State state);
    Optional<State> findById(Long id);
    List<State> findAll();
    List<State> findByCountryId(Long countryId);
    void delete(Long id);
    int reactivate(Long id);
}
