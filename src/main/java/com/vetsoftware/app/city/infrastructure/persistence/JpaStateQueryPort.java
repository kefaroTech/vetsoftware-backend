package com.vetsoftware.app.city.infrastructure.persistence;

import com.vetsoftware.app.city.application.port.out.StateQueryPort;
import com.vetsoftware.app.city.domain.StateRef;
import com.vetsoftware.app.state.infrastructure.persistence.StateJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaStateQueryPort implements StateQueryPort {
  private final StateJpaRepository stateJpaRepository;

  public JpaStateQueryPort(StateJpaRepository stateJpaRepository) {
    this.stateJpaRepository = stateJpaRepository;
  }

  @Override
  public Optional<StateRef> findById(Long stateId) {
    return stateJpaRepository.findById(stateId).map(e -> new StateRef(e.getId(), e.getName()));
  }
}
