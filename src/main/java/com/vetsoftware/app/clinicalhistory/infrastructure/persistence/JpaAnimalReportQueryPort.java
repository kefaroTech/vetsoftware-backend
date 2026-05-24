package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.clinicalhistory.application.dto.AnimalReportInfo;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalReportQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaAnimalReportQueryPort implements AnimalReportQueryPort {

    private final AnimalJpaRepository animalJpaRepository;

    public JpaAnimalReportQueryPort(AnimalJpaRepository animalJpaRepository) {
        this.animalJpaRepository = animalJpaRepository;
    }

    @Override
    public Optional<AnimalReportInfo> findByIdAndCompanyId(Long animalId, Long companyId) {
        return animalJpaRepository.findById(animalId)
                .filter(a -> a.getCompany() != null
                        && companyId.equals(a.getCompany().getId()))
                .map(a -> new AnimalReportInfo(
                        a.getId(),
                        a.getName(),
                        a.getCode(),
                        a.getSpecie().getName(),
                        a.getBreed().getName(),
                        a.getOwner().getName(),
                        a.getOwner().getPhone(),
                        a.getCompany().getName(),
                        a.getCompany().getIdentifier()
                ));
    }
}
