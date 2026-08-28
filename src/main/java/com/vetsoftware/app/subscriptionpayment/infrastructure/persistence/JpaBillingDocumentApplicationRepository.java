package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBillingDocumentApplicationRepository
        implements
            BillingDocumentApplicationRepository {

    private final BillingDocumentApplicationJpaRepository jpaRepository;
    private final BillingDocumentApplicationJpaMapper mapper;
    private final SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository;
    private final SubscriptionPaymentJpaRepository paymentJpaRepository;

    public JpaBillingDocumentApplicationRepository(
            BillingDocumentApplicationJpaRepository jpaRepository,
            BillingDocumentApplicationJpaMapper mapper,
            SubscriptionBillingDocumentJpaRepository billingDocumentJpaRepository,
            SubscriptionPaymentJpaRepository paymentJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.billingDocumentJpaRepository = billingDocumentJpaRepository;
        this.paymentJpaRepository = paymentJpaRepository;
    }

    /**
     * Escritura con {@code getReferenceById}: proxies sin {@code SELECT}, porque el
     * caso de uso ya resolvio y valido los {@code Ref} que necesita. Y el
     * {@code toDomain} de vuelta reusa esos mismos {@code Ref} en vez de leer del
     * proxy, que dispararia una consulta de hidratacion por cada asociacion.
     */
    @Override
    public BillingDocumentApplication save(BillingDocumentApplication application) {
        SubscriptionBillingDocumentJpaEntity targetDocument = billingDocumentJpaRepository
                .getReferenceById(application.getTargetDocument().id());
        SubscriptionPaymentJpaEntity payment = application.getPaymentId() == null
                ? null
                : paymentJpaRepository.getReferenceById(application.getPaymentId());
        SubscriptionBillingDocumentJpaEntity sourceDocument = application
                .getSourceDocument() == null
                        ? null
                        : billingDocumentJpaRepository
                                .getReferenceById(application.getSourceDocument().id());
        BillingDocumentApplicationJpaEntity reversalOf = application.getReversalOfId() == null
                ? null
                : jpaRepository.getReferenceById(application.getReversalOfId());
        BillingDocumentApplicationJpaEntity saved = jpaRepository.save(
                mapper.toJpa(application, targetDocument, payment, sourceDocument, reversalOf));
        return mapper.toDomain(saved, application.getTargetDocument(),
                application.getSourceDocument());
    }

    @Override
    public Optional<BillingDocumentApplication> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public BigDecimal sumAppliedFromPayment(Long paymentId, Long companyId) {
        return jpaRepository.sumAppliedFromPayment(paymentId, companyId);
    }

    @Override
    public BigDecimal sumAppliedFromSourceDocument(Long sourceDocumentId, Long companyId) {
        return jpaRepository.sumAppliedFromSourceDocument(sourceDocumentId, companyId);
    }

    @Override
    public BigDecimal sumAppliedFromWithholding(Long withholdingId, Long companyId) {
        return jpaRepository.sumAppliedFromWithholding(withholdingId, companyId);
    }

    @Override
    public BigDecimal sumAppliedFromCreditEntry(Long creditEntryId, Long companyId) {
        return jpaRepository.sumAppliedFromCreditEntry(creditEntryId, companyId);
    }

    @Override
    public Optional<BillingDocumentApplication> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId) {
        return jpaRepository.findByCompanyIdAndClientRequestId(companyId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<BillingDocumentApplication> findByReversalOfIdAndCompanyId(Long reversalOfId,
            Long companyId) {
        return jpaRepository.findByReversalOf_IdAndCompanyId(reversalOfId, companyId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Long> findTargetDocumentIdsByPaymentId(Long paymentId, Long companyId) {
        return jpaRepository.findTargetDocumentIdsByPaymentId(paymentId, companyId);
    }

    /**
     * Orden total: {@code appliedAt} ordena el expediente y el {@code id} desempata
     * dentro del mismo microsegundo, que es exactamente el caso de un pago que
     * salda tres facturas de un giro.
     */
    @Override
    public PageResult<BillingDocumentApplication> findAllByTargetDocumentIdAndCompanyId(
            Long targetDocumentId, Long companyId, int page, int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "appliedAt")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(jpaRepository.findAllByTargetDocument_IdAndCompanyId(targetDocumentId,
                companyId, Pages.request(page, pageSize, order)), mapper::toDomain);
    }
}
