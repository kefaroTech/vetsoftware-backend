package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.quote.application.dto.QuoteTotalsMismatchDto;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.domain.QuoteSummary;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaQuoteRepository implements QuoteRepository {

    /**
     * Lo que todavia puede vencer. Una cotizacion ya aceptada, rechazada o vencida
     * no vuelve a moverse por el barrido.
     */
    private static final List<QuoteStatus> EXPIRABLE = List.of(QuoteStatus.DRAFT, QuoteStatus.SENT);

    private final QuoteJpaRepository jpaRepository;
    private final QuoteJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaQuoteRepository(QuoteJpaRepository jpaRepository, QuoteJpaMapper mapper,
            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Quote save(Quote quote) {
        // getReferenceById devuelve un proxy sin SELECT; null cuando la cotizacion
        // es a un prospecto, que es un caso legitimo y no una FK que falte.
        CompanyJpaEntity company = quote.getCompany() == null
                ? null
                : companyJpaRepository.getReferenceById(quote.getCompany().id());
        QuoteJpaEntity saved = jpaRepository.saveAndFlush(mapper.toJpa(quote, company));
        return mapper.toDomain(saved, quote.getCompany());
    }

    @Override
    public Optional<Quote> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Quote> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<Quote> findByClientRequestId(String clientRequestId) {
        return jpaRepository.findByClientRequestId(clientRequestId).map(mapper::toDomain);
    }

    @Override
    public Optional<Quote> findByClientRequestIdAndCompanyId(String clientRequestId,
            Long companyId) {
        return jpaRepository.findByClientRequestIdAndCompany_Id(clientRequestId, companyId)
                .map(mapper::toDomain);
    }

    /**
     * El orden es total -fecha de creacion descendente con desempate por id-, y eso
     * no es una floritura: sin desempate estable, dos paginas consecutivas repiten
     * u omiten filas cuando varias cotizaciones caen en el mismo segundo.
     */
    @Override
    public PageResult<QuoteSummary> findAllByCompanyId(Long companyId, int page, int pageSize) {
        Page<QuoteJpaEntity> result = jpaRepository.findAllByCompany_Id(companyId,
                Pages.request(page, pageSize, newestFirst()));
        return Pages.result(result, mapper::toSummary);
    }

    @Override
    public PageResult<QuoteSummary> findAll(int page, int pageSize) {
        Page<QuoteJpaEntity> result = jpaRepository
                .findAll(Pages.request(page, pageSize, newestFirst()));
        return Pages.result(result, mapper::toSummary);
    }

    /**
     * Dos pasos: primero los ids, acotados en la base; despues el detalle. Traer
     * las colecciones en la misma consulta paginada obligaria a Hibernate a
     * recortar en memoria y el batchSize dejaria de significar nada.
     */
    @Override
    public List<Quote> findExpirable(LocalDate today, int batchSize) {
        Page<Long> ids = jpaRepository.findExpirableIds(EXPIRABLE, today,
                Pages.request(0, batchSize));
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByIdIn(ids.getContent()).stream().map(mapper::toDomain)
                .sorted(Comparator.comparing(Quote::getValidUntil).thenComparing(Quote::getId))
                .toList();
    }

    /**
     * Object[] a mano y no una proyeccion de interfaz: son nueve columnas agregadas
     * de dos tablas, no una entidad, y darle nombre a la proyeccion no ahorra nada
     * aqui. Los importes salen a BigDecimal por String para no pasar por double,
     * que es como se pierde un centavo en una alerta que existe justamente para
     * contar centavos.
     */
    @Override
    public List<QuoteTotalsMismatchDto> findAllTotalsMismatches() {
        return jpaRepository.findAllTotalsMismatches().stream()
                .map(row -> new QuoteTotalsMismatchDto(id(row[0]), text(row[1]), id(row[2]),
                        amount(row[3]), amount(row[4]), amount(row[5]), amount(row[6]),
                        amount(row[7]), amount(row[8])))
                .toList();
    }

    private static Long id(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static java.math.BigDecimal amount(Object value) {
        return value == null
                ? java.math.BigDecimal.ZERO
                : new java.math.BigDecimal(String.valueOf(value));
    }

    @Override
    public void softDelete(Long id, Long companyId) {
        jpaRepository.softDelete(id, companyId);
    }

    @Override
    public void softDelete(Long id) {
        jpaRepository.softDelete(id);
    }

    private static Sort newestFirst() {
        return Sort.by(Sort.Direction.DESC, "createdDate").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
