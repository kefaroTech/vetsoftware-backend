package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animal.domain.SpecieRef;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("animalJpaSpecieQueryPort")
public class JpaSpecieQueryPort implements SpecieQueryPort {
    private final SpecieJpaRepository specieJpaRepository;

    public JpaSpecieQueryPort(SpecieJpaRepository specieJpaRepository) {
        this.specieJpaRepository = specieJpaRepository;
    }

    @Override
    public Optional<SpecieRef> findById(Long specieId) {
        return specieJpaRepository.findById(specieId)
                .map(e -> new SpecieRef(e.getId(), e.getName()));
    }
}
