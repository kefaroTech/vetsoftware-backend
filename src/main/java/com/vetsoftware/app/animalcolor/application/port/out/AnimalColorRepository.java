package com.vetsoftware.app.animalcolor.application.port.out;

import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import java.util.List;
import java.util.Optional;

public interface AnimalColorRepository {
    AnimalColor save(AnimalColor color);

    Optional<AnimalColor> findById(Long id);

    List<AnimalColor> findAll();

    List<AnimalColor> findBySpecieId(Long specieId);

    void delete(Long id);

    int reactivate(Long id);
}
