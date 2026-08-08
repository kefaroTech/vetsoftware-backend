package com.vetsoftware.app.problem.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.problem.application.port.out.ProblemRepository;
import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.application.dto.PageResult;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProblemRepository implements ProblemRepository {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final ProblemJpaRepository jpaRepository;
    private final ProblemJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaProblemRepository(ProblemJpaRepository jpaRepository, ProblemJpaMapper mapper,
            AnimalJpaRepository animalJpaRepository, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Problem save(Problem problem) {
        AnimalJpaEntity animal = animalJpaRepository.getReferenceById(problem.getAnimal().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(problem.getCompany().id());
        ProblemJpaEntity saved = jpaRepository.save(mapper.toJpa(problem, animal, company));
        return mapper.toDomain(saved, problem.getAnimal(), problem.getCompany());
    }

    @Override
    public Optional<Problem> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<Problem> findByAnimalIdAndCompanyId(Long animalId, Long companyId, int page,
            int pageSize) {
        int safeSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        // El orden ya lo fija el nombre del metodo derivado (createdDate desc), asi que
        // el PageRequest no lo repite: lo contrario daria dos ORDER BY en conflicto.
        Page<ProblemJpaEntity> result = jpaRepository
                .findByAnimal_IdAndCompany_IdOrderByCreatedDateDesc(animalId, companyId,
                        PageRequest.of(Math.max(page, 0), safeSize));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.findByIdAndCompany_Id(id, companyId).ifPresent(jpaRepository::delete);
    }
}
