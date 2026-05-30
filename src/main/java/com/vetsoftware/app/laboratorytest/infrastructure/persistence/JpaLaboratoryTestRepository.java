package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaEntity;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaLaboratoryTestRepository implements LaboratoryTestRepository {
    private final LaboratoryTestJpaRepository jpaRepository;
    private final LaboratoryTestJpaMapper mapper;
    private final LaboratoryTestTypeJpaRepository testTypeJpaRepository;
    private final AnimalJpaRepository animalJpaRepository;
    private final ConsultationJpaRepository consultationJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaLaboratoryTestRepository(LaboratoryTestJpaRepository jpaRepository,
                                       LaboratoryTestJpaMapper mapper,
                                       LaboratoryTestTypeJpaRepository testTypeJpaRepository,
                                       AnimalJpaRepository animalJpaRepository,
                                       ConsultationJpaRepository consultationJpaRepository,
                                       CompanyJpaRepository companyJpaRepository,
                                       EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.testTypeJpaRepository = testTypeJpaRepository;
        this.animalJpaRepository = animalJpaRepository;
        this.consultationJpaRepository = consultationJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public LaboratoryTest save(LaboratoryTest laboratoryTest) {
        LaboratoryTestTypeJpaEntity testType = testTypeJpaRepository.getReferenceById(laboratoryTest.getTestType().id());
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(laboratoryTest.getAnimal().id());
        ConsultationJpaEntity consultation = laboratoryTest.getConsultation() == null ? null
            : consultationJpaRepository.getReferenceById(laboratoryTest.getConsultation().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(laboratoryTest.getCompany().id());
        EmployeeJpaEntity processedBy = laboratoryTest.getProcessedBy() == null ? null
            : employeeJpaRepository.getReferenceById(laboratoryTest.getProcessedBy().id());
        LaboratoryTestJpaEntity saved = jpaRepository.save(
            mapper.toJpa(laboratoryTest, testType, animal, consultation, company, processedBy));
        return mapper.toDomain(saved, laboratoryTest.getTestType(),
                                laboratoryTest.getAnimal(), laboratoryTest.getConsultation(),
                                laboratoryTest.getCompany(), laboratoryTest.getProcessedBy());
    }

    @Override
    public Optional<LaboratoryTest> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<LaboratoryTest> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<LaboratoryTest> findAllByAnimalId(Long animalId) {
        return jpaRepository.findAllByAnimalId(animalId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
