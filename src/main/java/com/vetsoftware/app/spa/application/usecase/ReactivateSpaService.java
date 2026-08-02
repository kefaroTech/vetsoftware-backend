package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.in.ReactivateSpaUseCase;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa.reactivate")
@Service
public class ReactivateSpaService implements ReactivateSpaUseCase {
  private final SpaRepository repository;

  public ReactivateSpaService(SpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SpaDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new SpaNotFoundException(id);
    return SpaDto.from(repository.findById(id).orElseThrow(() -> new SpaNotFoundException(id)));
  }
}
