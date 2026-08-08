package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.dto.PageResult;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDebtOpenAccountRepository implements DebtOpenAccountRepository {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private final DebtOpenAccountJpaRepository jpaRepository;
    private final DebtOpenAccountJpaMapper mapper;
    private final OpenAccountJpaRepository openAccountJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaDebtOpenAccountRepository(DebtOpenAccountJpaRepository jpaRepository,
            DebtOpenAccountJpaMapper mapper, OpenAccountJpaRepository openAccountJpaRepository,
            EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.openAccountJpaRepository = openAccountJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public DebtOpenAccount save(DebtOpenAccount debtOpenAccount) {
        OpenAccountJpaEntity openAccount = openAccountJpaRepository
                .getReferenceById(debtOpenAccount.getOpenAccount().id());
        EmployeeJpaEntity createdBy = employeeJpaRepository
                .getReferenceById(debtOpenAccount.getCreatedBy().id());
        EmployeeJpaEntity voidedBy = debtOpenAccount.getVoidedBy() == null
                ? null
                : employeeJpaRepository.getReferenceById(debtOpenAccount.getVoidedBy().id());
        DebtOpenAccountJpaEntity saved = jpaRepository
                .save(mapper.toJpa(debtOpenAccount, openAccount, createdBy, voidedBy));
        return mapper.toDomain(saved, debtOpenAccount.getOpenAccount(),
                debtOpenAccount.getCreatedBy(), debtOpenAccount.getVoidedBy());
    }

    @Override
    public Optional<DebtOpenAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DebtOpenAccount> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<DebtOpenAccount> findByOpenAccountIdAndClientRequestId(Long openAccountId,
            String clientRequestId) {
        return jpaRepository.findByOpenAccount_IdAndClientRequestId(openAccountId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public List<DebtOpenAccount> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<DebtOpenAccount> findAllByCompanyId(Long companyId, int page, int pageSize) {
        Page<DebtOpenAccountJpaEntity> result = jpaRepository
                .findAllByOpenAccount_Company_Id(companyId, pageRequest(page, pageSize));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * Normaliza lo que llega del cliente: una pagina negativa o un tamano desmedido
     * no deben poder volver a pedir la tabla entera, que es el fallo que se esta
     * corrigiendo. El orden por id descendente es estable y devuelve primero lo mas
     * reciente; sin orden explicito la paginacion no es determinista y una misma
     * fila puede salir en dos paginas.
     */
    private static PageRequest pageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Override
    public List<DebtOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId,
            Long companyId) {
        return jpaRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(openAccountId, companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId)
                .ifPresent(jpaRepository::delete);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
