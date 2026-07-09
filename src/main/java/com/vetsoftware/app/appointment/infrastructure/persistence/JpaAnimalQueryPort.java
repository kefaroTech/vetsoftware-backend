package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.domain.AnimalRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("appointmentJpaAnimalQueryPort")
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
