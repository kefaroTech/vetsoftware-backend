package com.vetsoftware.app.customercredit.infrastructure.persistence;

import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditBalanceRepository;
import com.vetsoftware.app.customercredit.domain.CustomerCreditBalance;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCustomerCreditBalanceRepository implements CustomerCreditBalanceRepository {

    private final CustomerCreditBalanceJpaRepository jpaRepository;
    private final CustomerCreditBalanceJpaMapper mapper;

    public JpaCustomerCreditBalanceRepository(CustomerCreditBalanceJpaRepository jpaRepository,
            CustomerCreditBalanceJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CustomerCreditBalance> findByCompanyId(Long companyId) {
        return jpaRepository.findByCompanyId(companyId).map(mapper::toDomain);
    }

    @Override
    public int applyDelta(Long companyId, BigDecimal delta, LocalDateTime now) {
        return jpaRepository.applyDelta(companyId, delta, now);
    }

    @Override
    public void openIfAbsent(Long companyId, LocalDateTime now) {
        jpaRepository.openIfAbsent(companyId, now);
    }

    @Override
    public void refreshNextExpiry(Long companyId, LocalDate nextExpiryOn, LocalDateTime now) {
        jpaRepository.refreshNextExpiry(companyId, nextExpiryOn, now);
    }

    /**
     * Barrido de plataforma. Orden total: primero el saldo mas alto —que es el
     * pasivo mas grande y lo que se quiere ver arriba— y desempate por id.
     */
    @Override
    public PageResult<CustomerCreditBalance> findAll(int page, int pageSize) {
        Sort order = Sort.by(Sort.Direction.DESC, "balanceAmount")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order)),
                mapper::toDomain);
    }
}
