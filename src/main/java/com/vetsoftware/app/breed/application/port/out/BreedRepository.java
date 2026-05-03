package com.vetsoftware.app.breed.application.port.out;

import com.vetsoftware.app.breed.domain.Breed;
import java.util.List;
import java.util.Optional;

public interface BreedRepository {
    Breed save(Breed breed);
    Optional<Breed> findById(Long id);
    List<Breed> findAll();
    List<Breed> findBySpecieId(Long specieId);
    void delete(Long id);
}
