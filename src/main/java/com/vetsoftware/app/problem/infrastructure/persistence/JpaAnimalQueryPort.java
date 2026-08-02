package com.vetsoftware.app.problem.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.problem.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.problem.domain.AnimalRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("problemJpaAnimalQueryPort")
public class JpaAnimalQueryPort implements AnimalQueryPort {
    private final AnimalJpaRepository animalJpaRepository;

    public JpaAnimalQueryPort(AnimalJpaRepository animalJpaRepository) {
        this.animalJpaRepository = animalJpaRepository;
    }

    @Override
    public Optional<AnimalRef> findByIdAndCompanyId(Long animalId, Long companyId) {
        return animalJpaRepository.findByIdAndCompany_Id(animalId, companyId)
                .map(e -> new AnimalRef(e.getId(), e.getName(), e.getCode()));
    }
}
