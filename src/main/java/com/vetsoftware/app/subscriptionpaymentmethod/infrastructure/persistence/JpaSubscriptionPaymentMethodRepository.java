package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateStatus;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubscriptionPaymentMethodRepository implements SubscriptionPaymentMethodRepository {

    private final SubscriptionPaymentMethodJpaRepository jpaRepository;
    private final SubscriptionPaymentMethodJpaMapper mapper;

    public JpaSubscriptionPaymentMethodRepository(
            SubscriptionPaymentMethodJpaRepository jpaRepository,
            SubscriptionPaymentMethodJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SubscriptionPaymentMethod save(SubscriptionPaymentMethod paymentMethod) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(paymentMethod)));
    }

    @Override
    public Optional<SubscriptionPaymentMethod> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionPaymentMethod> findByGatewayAndToken(String gateway, String token) {
        return jpaRepository.findByGatewayAndToken(gateway, token).map(mapper::toDomain);
    }

    @Override
    public int clearDefaultForCompany(Long companyId, Long excludedId) {
        return jpaRepository.clearDefaultForCompany(companyId, excludedId, MandateStatus.ACTIVE);
    }

    /**
     * Orden total y estable: el predeterminado primero —es el que el cliente busca
     * al entrar—, despues lo mas recientemente autorizado, y el {@code id} como
     * desempate. Sin desempate, dos altas del mismo microsegundo pueden salir en
     * dos paginas o en ninguna.
     */
    @Override
    public PageResult<SubscriptionPaymentMethod> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(
                jpaRepository.findAllByCompanyId(companyId, Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    /** Lo que antes caduca, primero: es el orden en que hay que avisar. */
    @Override
    public PageResult<SubscriptionPaymentMethod> findAllExpiringBefore(LocalDate before, int page,
            int pageSize) {
        Sort expiringFirst = Sort.by(Sort.Direction.ASC, "expiresOn")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(jpaRepository.findAllExpiringBefore(before, MandateStatus.ACTIVE,
                Pages.request(page, pageSize, expiringFirst)), mapper::toDomain);
    }

    @Override
    public PageResult<SubscriptionPaymentMethod> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    private static Sort order() {
        return Sort.by(Sort.Direction.DESC, "defaultMethod")
                .and(Sort.by(Sort.Direction.DESC, "authorizedAt"))
                .and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
