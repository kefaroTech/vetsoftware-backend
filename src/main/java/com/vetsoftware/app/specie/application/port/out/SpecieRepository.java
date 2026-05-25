package com.vetsoftware.app.specie.application.port.out;

import com.vetsoftware.app.specie.domain.Specie;
import java.util.List;
import java.util.Optional;

public interface SpecieRepository {
    Specie save(Specie specie);
    Optional<Specie> findById(Long id);
    List<Specie> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
