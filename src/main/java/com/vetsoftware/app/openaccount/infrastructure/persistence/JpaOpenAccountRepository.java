package com.vetsoftware.app.openaccount.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
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
public class JpaOpenAccountRepository implements OpenAccountRepository {

    private static final int LIST_DEFAULT_PAGE_SIZE = 20;
    private static final int LIST_MAX_PAGE_SIZE = 200;
    private final OpenAccountJpaRepository jpaRepository;
    private final OpenAccountJpaMapper mapper;
    private final OwnerJpaRepository ownerJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;
    private final BranchJpaRepository branchJpaRepository;

    public JpaOpenAccountRepository(OpenAccountJpaRepository jpaRepository,
            OpenAccountJpaMapper mapper, OwnerJpaRepository ownerJpaRepository,
            CompanyJpaRepository companyJpaRepository, EmployeeJpaRepository employeeJpaRepository,
            BranchJpaRepository branchJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.ownerJpaRepository = ownerJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
        this.branchJpaRepository = branchJpaRepository;
    }

    @Override
    public OpenAccount save(OpenAccount openAccount) {
        OwnerJpaEntity owner = ownerJpaRepository.getReferenceById(openAccount.getOwner().id());
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(openAccount.getCompany().id());
        BranchJpaEntity branch = branchJpaRepository.getReferenceById(openAccount.getBranch().id());
        EmployeeJpaEntity createdBy = employeeJpaRepository
                .getReferenceById(openAccount.getCreatedBy().id());
        EmployeeJpaEntity closedBy = openAccount.getClosedBy() != null
                ? employeeJpaRepository.getReferenceById(openAccount.getClosedBy().id())
                : null;
        OpenAccountJpaEntity saved = jpaRepository
                .save(mapper.toJpa(openAccount, owner, company, branch, createdBy, closedBy));
        return mapper.toDomain(saved, openAccount.getOwner(), openAccount.getCompany(),
                openAccount.getBranch(), openAccount.getCreatedBy(), openAccount.getClosedBy());
    }

    @Override
    public Optional<OpenAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<OpenAccount> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<OpenAccount> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<OpenAccount> findByIdForUpdateAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdForUpdateAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<OpenAccount> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<OpenAccount> findAllByCompanyId(Long companyId, Long branchId, int page,
            int pageSize) {
        Page<OpenAccountJpaEntity> result = jpaRepository.findByCompanyIdAndOptionalBranch(
                companyId, branchId, listPageRequest(page, pageSize));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * Normaliza lo que llega del cliente y acota el tamano: sin tope se vuelve a
     * pedir la tabla entera por query param. Orden por id descendente, estable y
     * con lo mas reciente primero.
     *
     * <p>
     * El grafo de la consulta es todo to-one (owner, company, branch, createdBy),
     * asi que el JOIN FETCH convive con la paginacion. Con una coleccion Hibernate
     * paginaria en memoria.
     */
    private static PageRequest listPageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0
                ? LIST_DEFAULT_PAGE_SIZE
                : Math.min(pageSize, LIST_MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Override
    public boolean existsActiveByOwnerIdAndBranchId(Long ownerId, Long branchId) {
        return jpaRepository.existsByOwnerIdAndBranchIdAndStatusAndEnabledTrue(ownerId, branchId,
                OpenAccountStatus.OPEN);
    }

    @Override
    public PageResult<OpenAccount> search(SearchOpenAccountsCommand command) {
        Specification<OpenAccountJpaEntity> spec = buildSpec(command);
        PageRequest pageRequest = PageRequest.of(Math.max(command.page(), 0),
                command.pageSize() <= 0 ? 20 : command.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<OpenAccountJpaEntity> page = jpaRepository.findAll(spec, pageRequest);
        List<OpenAccount> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    private Specification<OpenAccountJpaEntity> buildSpec(SearchOpenAccountsCommand command) {
        return (root, query, cb) -> {
            // fetch-join de los @ManyToOne solo en la query de datos (no en la de count)
            // para evitar N+1
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("owner", JoinType.LEFT);
                root.fetch("company", JoinType.LEFT);
                root.fetch("branch", JoinType.LEFT);
                root.fetch("createdBy", JoinType.LEFT);
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("id"), command.companyId()));
            if (command.ownerId() != null) {
                predicates.add(cb.equal(root.get("owner").get("id"), command.ownerId()));
            }
            if (command.enabled() != null) {
                predicates.add(cb.equal(root.get("enabled"), command.enabled()));
            }
            // Multi-sucursal (Fase C): filtro opcional por sede.
            if (command.branchId() != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), command.branchId()));
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
