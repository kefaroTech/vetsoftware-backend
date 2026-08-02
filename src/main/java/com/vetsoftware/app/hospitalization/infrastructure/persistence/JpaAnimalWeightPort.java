package com.vetsoftware.app.hospitalization.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.hospitalization.application.port.out.AnimalWeightPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class JpaAnimalWeightPort implements AnimalWeightPort {
    private final WeightRecordJpaRepository weightRecordJpaRepository;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaAnimalWeightPort(WeightRecordJpaRepository weightRecordJpaRepository,
            AnimalJpaRepository animalJpaRepository, CompanyJpaRepository companyJpaRepository) {
        this.weightRecordJpaRepository = weightRecordJpaRepository;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public void recordHospitalizationWeight(Long animalId, Long companyId, BigDecimal value,
            String unit, LocalDate measuredAt, Long hospitalizationId) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(animalId);
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(companyId);
        weightRecordJpaRepository.save(WeightRecordJpaEntity.of(animal, company, value, unit,
                "HOSPITALIZATION", hospitalizationId, null, measuredAt));
    }
}
