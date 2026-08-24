package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubscriptionAmendmentRepository implements SubscriptionAmendmentRepository {

    private static final String CLIENT_REQUEST_CONSTRAINT = "uq_subscription_amendments_client_request";

    private final SubscriptionAmendmentJpaRepository jpaRepository;
    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionAmendmentJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaSubscriptionAmendmentRepository(SubscriptionAmendmentJpaRepository jpaRepository,
            SubscriptionJpaRepository subscriptionJpaRepository,
            SubscriptionAmendmentJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public SubscriptionAmendment save(SubscriptionAmendment amendment) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(amendment.getCompanyId());
        SubscriptionJpaEntity subscription = subscriptionJpaRepository
                .getReferenceById(amendment.getSubscriptionId());
        try {
            return mapper.toDomain(
                    jpaRepository.saveAndFlush(mapper.toJpa(amendment, company, subscription)));
        } catch (DataIntegrityViolationException exception) {
            // El camino idempotente NO es este: los casos de uso buscan el
            // clientRequestId antes de insertar, dentro de la transaccion. Esto es la
            // carrera —dos peticiones simultaneas con la misma llave, las dos pasan la
            // busqueda— y se resuelve devolviendo lo que gano, que sigue siendo la
            // respuesta idempotente correcta.
            if (violates(exception, CLIENT_REQUEST_CONSTRAINT)) {
                return findByClientRequestIdAndCompanyId(amendment.getClientRequestId(),
                        amendment.getCompanyId()).orElseThrow(() -> exception);
            }
            throw exception;
        }
    }

    @Override
    public Optional<SubscriptionAmendment> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionAmendment> findByClientRequestIdAndCompanyId(String clientRequestId,
            Long companyId) {
        if (clientRequestId == null || clientRequestId.isBlank())
            return Optional.empty();
        return jpaRepository.findByClientRequestIdAndCompany_Id(clientRequestId, companyId)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<SubscriptionAmendment> findAllBySubscriptionIdAndCompanyId(
            Long subscriptionId, Long companyId, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllBySubscription_IdAndCompany_Id(subscriptionId,
                companyId, Pages.request(page, pageSize, order())), mapper::toDomain);
    }

    /** La pelicula en orden, con desempate por id. */
    private static Sort order() {
        return Sort.by(Sort.Direction.ASC, "effectiveDate").and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraint) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        return message != null && message.toLowerCase().contains(constraint);
    }
}
