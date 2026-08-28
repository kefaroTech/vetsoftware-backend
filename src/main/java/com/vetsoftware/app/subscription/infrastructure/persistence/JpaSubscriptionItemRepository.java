package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemOverlapDto;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubscriptionItemRepository implements SubscriptionItemRepository {

    /**
     * El indice unico sobre {@code current_item_marker}: impide dos lineas abiertas
     * del mismo articulo en el mismo contrato. Es la ultima linea de defensa del
     * caso exacto; la primera es el bloqueo sobre {@code subscriptions} mas la
     * comprobacion de solape del caso de uso, que ademas cubre los tramos con fecha
     * de fin futura que este indice no puede ver.
     */
    private static final String CURRENT_ITEM_CONSTRAINT = "uq_subscription_items_current";

    private final SubscriptionItemJpaRepository jpaRepository;
    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionItemJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaSubscriptionItemRepository(SubscriptionItemJpaRepository jpaRepository,
            SubscriptionJpaRepository subscriptionJpaRepository, SubscriptionItemJpaMapper mapper,
            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public SubscriptionItem save(SubscriptionItem item) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(item.getCompanyId());
        SubscriptionJpaEntity subscription = subscriptionJpaRepository
                .getReferenceById(item.getSubscriptionId());
        try {
            return mapper.toDomain(
                    jpaRepository.saveAndFlush(mapper.toJpa(item, company, subscription)));
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, CURRENT_ITEM_CONSTRAINT)) {
                throw new SubscriptionItemOverlapException(item.getCatalogItemId(),
                        item.getPeriod().from(), item.getPeriod().to());
            }
            throw exception;
        }
    }

    @Override
    public List<SubscriptionItem> saveAll(List<SubscriptionItem> items) {
        return items.stream().map(this::save).toList();
    }

    @Override
    public Optional<SubscriptionItem> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionItem> findByCreatedAmendmentIdAndCompanyId(Long amendmentId,
            Long companyId) {
        return jpaRepository.findByCreatedAmendmentIdAndCompany_Id(amendmentId, companyId)
                .map(mapper::toDomain);
    }

    @Override
    public List<SubscriptionItem> findAllByCreatedAmendmentIdAndCompanyId(Long amendmentId,
            Long companyId) {
        return jpaRepository
                .findAllByCreatedAmendmentIdAndCompany_IdOrderByTierMinAsc(amendmentId, companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SubscriptionItem> findOverlapping(Long companyId, Long subscriptionId,
            Long catalogItemId, LocalDate from, LocalDate to, Long excludeItemId) {
        return jpaRepository.findOverlapping(companyId, subscriptionId, catalogItemId, from, to,
                excludeItemId, EffectivePeriod.OPEN_ENDED).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SubscriptionItem> findOpenByCatalogItemId(Long companyId, Long subscriptionId,
            Long catalogItemId) {
        return jpaRepository.findByCompany_IdAndSubscription_IdAndCatalogItemIdAndEffectiveToIsNull(
                companyId, subscriptionId, catalogItemId).map(mapper::toDomain);
    }

    @Override
    public PageResult<SubscriptionItem> findAllBySubscriptionIdAndCompanyId(Long subscriptionId,
            Long companyId, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllBySubscription_IdAndCompany_Id(subscriptionId,
                companyId, Pages.request(page, pageSize, order())), mapper::toDomain);
    }

    @Override
    public PageResult<SubscriptionItem> findCurrentOn(Long subscriptionId, Long companyId,
            LocalDate day, int page, int pageSize) {
        return Pages.result(jpaRepository.findCurrentOn(companyId, subscriptionId, day,
                Pages.request(page, pageSize, order())), mapper::toDomain);
    }

    @Override
    public List<SubscriptionItem> findAllCurrentOn(Long subscriptionId, Long companyId,
            LocalDate day) {
        return jpaRepository.findAllCurrentOn(companyId, subscriptionId, day).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<SubscriptionItemOverlapDto> findAllOverlaps() {
        return jpaRepository.findAllOverlaps().stream()
                .map(row -> new SubscriptionItemOverlapDto(row.getCompanyId(),
                        row.getSubscriptionId(), row.getCatalogItemId(), row.getItemCode(),
                        row.getFirstItemId(), row.getFirstFrom(), row.getFirstTo(),
                        row.getSecondItemId(), row.getSecondFrom(), row.getSecondTo()))
                .toList();
    }

    /** El expediente se lee en orden cronologico, con desempate por id. */
    private static Sort order() {
        return Sort.by(Sort.Direction.ASC, "effectiveFrom").and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraint) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        return message != null && message.toLowerCase().contains(constraint);
    }
}
