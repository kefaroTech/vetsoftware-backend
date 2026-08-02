package com.vetsoftware.app.city.application.usecase;

import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.in.ReactivateCityUseCase;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.domain.CityNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "city.reactivate")
@Service
public class ReactivateCityService implements ReactivateCityUseCase {
  private final CityRepository repository;

  public ReactivateCityService(CityRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public CityDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new CityNotFoundException(id);
    return CityDto.from(repository.findById(id).orElseThrow(() -> new CityNotFoundException(id)));
  }
}
