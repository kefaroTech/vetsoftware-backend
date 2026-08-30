package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.CompanyAlreadyHasActiveSubscriptionException;
import com.vetsoftware.app.subscription.domain.QuoteAlreadyConvertedException;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubscriptionRepository implements SubscriptionRepository {

    /**
     * El indice unico sobre la columna generada {@code active_marker}. Es la unica
     * autoridad sobre «una empresa, un contrato vivo»: comprobarlo antes con un
     * {@code SELECT} seria una carrera, porque dos altas simultaneas leerian las
     * dos «no hay» e insertarian las dos.
     */
    private static final String ACTIVE_COMPANY_CONSTRAINT = "uq_subscriptions_active_company";

    /**
     * El indice unico sobre {@code quote_id} (changeset 391): una cotizacion, un
     * contrato. Cierra la carrera que la guarda de
     * {@code ReplaceSubscriptionFromQuoteService} no puede cerrar —dos aceptaciones
     * simultaneas de la misma oferta— por la misma razon que la constante de
     * arriba: un {@code SELECT} previo y un {@code INSERT} despues no serializan
     * nada.
     *
     * <p>
     * El compuesto {@code (company_id, quote_id)} que respalda la clave foranea NO
     * lo cubria: empieza por la empresa, asi que dos filas con la misma cotizacion
     * y la misma empresa pasan por el sin chocar.
     */
    private static final String QUOTE_CONSTRAINT = "uq_subscriptions_quote";

    private final SubscriptionJpaRepository jpaRepository;
    private final SubscriptionJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaSubscriptionRepository(SubscriptionJpaRepository jpaRepository,
            SubscriptionJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Subscription save(Subscription subscription) {
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(subscription.getCompanyId());
        try {
            // saveAndFlush y no save: sin el flush la violacion de unique saltaria en el
            // commit, fuera de este try, y el conflicto de negocio saldria como un 500.
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(subscription, company)));
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, ACTIVE_COMPANY_CONSTRAINT)) {
                throw new CompanyAlreadyHasActiveSubscriptionException(subscription.getCompanyId());
            }
            // La hermana: misma forma, otro invariante. Se comprueba aparte y no con un
            // OR porque los dos conflictos son distintos para quien los lee -«ya tienes
            // contrato» y «esa oferta ya se firmo»- y colapsarlos en un mensaje devuelve
            // al cliente a adivinar cual de los dos le paso.
            if (violates(exception, QUOTE_CONSTRAINT)) {
                throw new QuoteAlreadyConvertedException(subscription.getQuoteId());
            }
            throw exception;
        }
    }

    @Override
    public Optional<Subscription> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<Subscription> findCurrentByCompanyId(Long companyId) {
        // El criterio de vigente vive en SubscriptionStatus.CURRENT y en ningun otro
        // sitio: es el mismo conjunto que alimenta active_marker.
        return jpaRepository.findFirstByCompany_IdAndStatusIn(companyId, SubscriptionStatus.CURRENT)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Subscription> lockByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.lockByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Subscription> lockLifecycleBatchAfter(long afterId, int batchSize) {
        return jpaRepository.lockLifecycleBatchAfter(afterId, batchSize).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Subscription> findAllByCompanyId(Long companyId, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByCompany_Id(companyId,
                Pages.request(page, pageSize, order())), mapper::toDomain);
    }

    @Override
    public PageResult<Subscription> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    /**
     * El contrato mas reciente primero, con desempate por id: sin un orden total,
     * dos paginas consecutivas repiten u omiten filas.
     */
    private static Sort order() {
        return Sort.by(Sort.Direction.DESC, "startDate").and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraint) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        return message != null && message.toLowerCase().contains(constraint);
    }
}
