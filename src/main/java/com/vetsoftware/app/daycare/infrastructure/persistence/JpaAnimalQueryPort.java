package com.vetsoftware.app.daycare.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.daycare.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.daycare.domain.AnimalRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("dayCareJpaAnimalQueryPort")
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
