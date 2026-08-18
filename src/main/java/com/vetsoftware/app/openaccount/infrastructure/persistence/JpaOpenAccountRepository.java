package com.vetsoftware.app.openaccount.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountsSummaryDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
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
        // Orden por id descendente, estable y con lo mas reciente primero. El grafo de
        // la consulta es todo to-one (owner, company, branch, createdBy), asi que el
        // JOIN FETCH convive con la paginacion. Con una coleccion Hibernate paginaria
        // en memoria.
        Page<OpenAccountJpaEntity> result = jpaRepository.findByCompanyIdAndOptionalBranch(
                companyId, branchId,
                Pages.request(page, pageSize, Sort.by(Sort.Direction.DESC, "id")));
        return Pages.result(result, mapper::toDomain);
    }

    @Override
    public OpenAccountsSummaryDto summarize(Long companyId, Long branchId) {
        OpenAccountJpaRepository.OpenAccountsSummaryProjection row = jpaRepository
                .summarize(companyId, branchId, OpenAccountStatus.OPEN);
        BigDecimal pendiente = row.getTotalOutstanding();
        return new OpenAccountsSummaryDto(row.getOpenCount(), row.getClosedCount(),
                pendiente == null ? BigDecimal.ZERO : pendiente);
    }

    @Override
    public boolean existsActiveByOwnerIdAndBranchId(Long ownerId, Long branchId) {
        return jpaRepository.existsByOwnerIdAndBranchIdAndStatusAndEnabledTrue(ownerId, branchId,
                OpenAccountStatus.OPEN);
    }

    @Override
    public PageResult<OpenAccount> search(SearchOpenAccountsCommand command) {
        Specification<OpenAccountJpaEntity> spec = buildSpec(command);
        PageRequest pageRequest = Pages.request(command.page(), command.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<OpenAccountJpaEntity> page = jpaRepository.findAll(spec, pageRequest);
        return Pages.result(page, mapper::toDomain);
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
            // BE-06: la pestana de la pantalla. "Cerradas" son dos estados (cobrada y
            // cancelada), por eso es un IN y no una igualdad.
            if (!command.statuses().isEmpty()) {
                predicates.add(root.get("status").in(command.statuses()));
            }
            // BE-06: el buscador de la pantalla. Se compara en minusculas sobre nombre
            // y documento del propietario, que es exactamente lo que hacia el cliente.
            if (command.q() != null && !command.q().isBlank()) {
                String patron = "%" + command.q().trim().toLowerCase() + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("owner").get("name")), patron),
                        cb.like(cb.lower(root.get("owner").get("document")), patron)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
