package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.laboratorytest.application.command.SearchLaboratoryTestsCommand;
import com.vetsoftware.app.laboratorytest.application.dto.PageResult;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaEntity;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public Optional<LaboratoryTest> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
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
    public PageResult<LaboratoryTest> search(SearchLaboratoryTestsCommand command) {
        Specification<LaboratoryTestJpaEntity> spec = buildSpec(command);
        PageRequest pageRequest = PageRequest.of(
            Math.max(command.page(), 0),
            command.pageSize() <= 0 ? 20 : command.pageSize(),
            Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<LaboratoryTestJpaEntity> page = jpaRepository.findAll(spec, pageRequest);
        List<LaboratoryTest> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages());
    }

    private Specification<LaboratoryTestJpaEntity> buildSpec(SearchLaboratoryTestsCommand command) {
        return (root, query, cb) -> {
            // fetch-join de los @ManyToOne solo en la query de datos (no en la de count) para evitar N+1
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("testType", JoinType.LEFT);
                root.fetch("animal", JoinType.LEFT);
                root.fetch("consultation", JoinType.LEFT);
                root.fetch("company", JoinType.LEFT);
                root.fetch("processedBy", JoinType.LEFT);
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("id"), command.companyId()));
            if (command.branchId() != null) {
                predicates.add(cb.equal(root.get("branchId"), command.branchId()));
            }
            if (command.statuses() != null && !command.statuses().isEmpty()) {
                predicates.add(root.get("status").in(
                    command.statuses().stream().map(Enum::name).toList()));
            }
            if (command.animalId() != null) {
                predicates.add(cb.equal(root.get("animal").get("id"), command.animalId()));
            }
            if (command.testTypeId() != null) {
                predicates.add(cb.equal(root.get("testType").get("id"), command.testTypeId()));
            }
            if (command.prioridad() != null) {
                predicates.add(cb.equal(root.get("prioridad"), command.prioridad().name()));
            }
            if (command.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), command.dateFrom()));
            }
            if (command.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), command.dateTo()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
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
