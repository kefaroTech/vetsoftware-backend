package com.vetsoftware.app.animal.application.port.out;

import com.vetsoftware.app.animal.domain.Animal;
import java.util.List;
import java.util.Optional;

public interface AnimalRepository {
    Animal save(Animal animal);
    Optional<Animal> findById(Long id);
    List<Animal> findAll();
    void delete(Long id);
}
