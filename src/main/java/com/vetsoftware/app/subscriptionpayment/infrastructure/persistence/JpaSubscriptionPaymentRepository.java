package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubscriptionPaymentRepository implements SubscriptionPaymentRepository {

    private final SubscriptionPaymentJpaRepository jpaRepository;
    private final SubscriptionPaymentJpaMapper mapper;

    public JpaSubscriptionPaymentRepository(SubscriptionPaymentJpaRepository jpaRepository,
            SubscriptionPaymentJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SubscriptionPayment save(SubscriptionPayment payment) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(payment)));
    }

    @Override
    public Optional<SubscriptionPayment> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionPayment> lockByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.lockByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionPayment> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId) {
        return jpaRepository.findByCompanyIdAndClientRequestId(companyId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionPayment> findByGatewayAndGatewayReference(String gateway,
            String gatewayReference) {
        return jpaRepository.findByGatewayAndGatewayReference(gateway, gatewayReference)
                .map(mapper::toDomain);
    }

    /**
     * Orden total y estable: lo mas reciente primero por fecha de entrada, con el
     * {@code id} de desempate. Sin desempate, dos pagos del mismo microsegundo
     * pueden salir en dos paginas o en ninguna.
     */
    @Override
    public PageResult<SubscriptionPayment> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        Sort order = Sort.by(Sort.Direction.DESC, "receivedAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(
                jpaRepository.findAllByCompanyId(companyId, Pages.request(page, pageSize, order)),
                mapper::toDomain);
    }

    @Override
    public PageResult<SubscriptionPayment> findAll(int page, int pageSize) {
        Sort order = Sort.by(Sort.Direction.DESC, "receivedAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order)),
                mapper::toDomain);
    }

    @Override
    public List<SubscriptionPayment> findStalePendingGatewayPayments(LocalDateTime threshold,
            int limit) {
        Sort order = Sort.by(Sort.Direction.ASC, "receivedAt");
        return jpaRepository.findByStatusAndGatewayIsNotNullAndReceivedAtBeforeOrderByReceivedAtAsc(
                SubscriptionPaymentStatus.PENDING, threshold, Pages.request(0, limit, order))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<SubscriptionPayment> findAllFiltered(Long companyId,
            SubscriptionPaymentStatus status, LocalDateTime receivedFrom, LocalDateTime receivedTo,
            LocalDateTime pendingThreshold, int page, int pageSize) {
        Sort order = pendingThreshold != null
                ? Sort.by(Sort.Direction.ASC, "receivedAt").and(Sort.by(Sort.Direction.ASC, "id"))
                : Sort.by(Sort.Direction.DESC, "receivedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"));
        return Pages.result(jpaRepository.findAllFiltered(companyId, status, receivedFrom,
                receivedTo, pendingThreshold, Pages.request(page, pageSize, order)),
                mapper::toDomain);
    }

    @Override
    public List<SubscriptionPayment> findAllFilteredForExport(Long companyId,
            SubscriptionPaymentStatus status, LocalDateTime receivedFrom, LocalDateTime receivedTo,
            LocalDateTime pendingThreshold) {
        return jpaRepository.findAllFilteredForExport(companyId, status, receivedFrom, receivedTo,
                pendingThreshold).stream().map(mapper::toDomain).toList();
    }
}
