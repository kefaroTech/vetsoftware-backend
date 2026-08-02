package com.vetsoftware.app.consultation.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.application.port.out.AnimalWeightPort;
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
    public void recordConsultationWeight(Long animalId, Long companyId, BigDecimal value,
            String unit, LocalDate measuredAt, Long consultationId) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(animalId);
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(companyId);
        weightRecordJpaRepository.save(WeightRecordJpaEntity.of(animal, company, value, unit,
                "CONSULTATION", consultationId, null, measuredAt));
    }
}
