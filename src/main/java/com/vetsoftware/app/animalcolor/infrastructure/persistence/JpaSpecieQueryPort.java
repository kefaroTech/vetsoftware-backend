package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import com.vetsoftware.app.animalcolor.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animalcolor.domain.SpecieRef;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("animalColorJpaSpecieQueryPort")
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
