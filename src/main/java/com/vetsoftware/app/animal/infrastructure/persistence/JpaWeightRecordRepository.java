package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaWeightRecordRepository implements WeightRecordRepository {
    private final WeightRecordJpaRepository jpaRepository;
    private final WeightRecordJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaWeightRecordRepository(WeightRecordJpaRepository jpaRepository,
                                     WeightRecordJpaMapper mapper,
                                     AnimalJpaRepository animalJpaRepository,
                                     CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public WeightRecord save(WeightRecord record) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(record.getAnimal().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(record.getCompany().id());
        WeightRecordJpaEntity saved = jpaRepository.save(mapper.toJpa(record, animal, company));
        return mapper.toDomain(saved, record.getAnimal(), record.getCompany());
    }

    @Override
    public List<WeightRecord> findByAnimalIdAndCompanyId(Long animalId, Long companyId) {
        return jpaRepository.findByAnimal_IdAndCompany_IdOrderByMeasuredAtDescIdDesc(animalId, companyId)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<WeightRecord> findLatestByAnimalIdAndCompanyId(Long animalId, Long companyId) {
        return jpaRepository.findTopByAnimal_IdAndCompany_IdOrderByMeasuredAtDescIdDesc(animalId, companyId)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<WeightRecord> findByIdAndAnimalIdAndCompanyId(Long id, Long animalId, Long companyId) {
        return jpaRepository.findByIdAndAnimal_IdAndCompany_Id(id, animalId, companyId).map(mapper::toDomain);
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.findByIdAndCompany_Id(id, companyId).ifPresent(jpaRepository::delete);
    }
}
