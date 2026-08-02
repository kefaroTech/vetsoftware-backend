package com.vetsoftware.app.deworming.application.usecase;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.in.ReactivateDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "deworming.reactivate")
@Service
public class ReactivateDewormingService implements ReactivateDewormingUseCase {
  private final DewormingRepository repository;

  public ReactivateDewormingService(DewormingRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public DewormingDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new DewormingNotFoundException(id);
    return DewormingDto.from(
        repository.findById(id).orElseThrow(() -> new DewormingNotFoundException(id)));
  }
}
