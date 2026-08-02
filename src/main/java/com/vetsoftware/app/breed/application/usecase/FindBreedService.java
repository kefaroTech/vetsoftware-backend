package com.vetsoftware.app.breed.application.usecase;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.in.FindBreedUseCase;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "breed.find")
@Service
public class FindBreedService implements FindBreedUseCase {
  private final BreedRepository repository;

  public FindBreedService(BreedRepository repository) {
    this.repository = repository;
  }

  @Override
  public BreedDto findById(Long id) {
    return BreedDto.from(repository.findById(id).orElseThrow(() -> new BreedNotFoundException(id)));
  }
}
