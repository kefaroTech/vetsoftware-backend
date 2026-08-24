package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentTax;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de {@link BillingDocumentRepository}.
 *
 * <p>
 * La cabecera y su desglose se guardan juntos y se leen juntos: son el mismo
 * agregado. El desglose <b>solo se escribe cuando el documento nace</b>; en
 * cualquier guardado posterior —cambio de estado, referencia externa, saldo— no
 * se toca, que es lo que hace cierto el {@code E1_APPEND_ONLY} con el que esa
 * tabla queda exenta de {@code @Version}.
 */
@Repository
public class JpaBillingDocumentRepository implements BillingDocumentRepository {

    private final SubscriptionBillingDocumentJpaRepository jpaRepository;
    private final SubscriptionBillingDocumentTaxJpaRepository taxJpaRepository;
    private final SubscriptionBillingDocumentJpaMapper mapper;
    private final SubscriptionBillingDocumentTaxJpaMapper taxMapper;

    public JpaBillingDocumentRepository(SubscriptionBillingDocumentJpaRepository jpaRepository,
            SubscriptionBillingDocumentTaxJpaRepository taxJpaRepository,
            SubscriptionBillingDocumentJpaMapper mapper,
            SubscriptionBillingDocumentTaxJpaMapper taxMapper) {
        this.jpaRepository = jpaRepository;
        this.taxJpaRepository = taxJpaRepository;
        this.mapper = mapper;
        this.taxMapper = taxMapper;
    }

    @Override
    public SubscriptionBillingDocument save(SubscriptionBillingDocument document) {
        boolean esNuevo = document.getId() == null;
        SubscriptionBillingDocumentJpaEntity saved = jpaRepository.save(mapper.toJpa(document));
        List<BillingDocumentTax> taxes = esNuevo
                ? guardarDesglose(document, saved.getId())
                : leerDesglose(saved.getId(), saved.getCompanyId());
        return mapper.toDomain(saved, taxes);
    }

    /**
     * Escribe el desglose una sola vez, ya atado al id de la cabecera que acaba de
     * generar la base.
     */
    private List<BillingDocumentTax> guardarDesglose(SubscriptionBillingDocument document,
            Long documentId) {
        List<SubscriptionBillingDocumentTaxJpaEntity> filas = document.getTaxes().stream()
                .map(tax -> taxMapper.toJpa(tax.attachedTo(documentId))).toList();
        return taxJpaRepository.saveAll(filas).stream().map(taxMapper::toDomain).toList();
    }

    private List<BillingDocumentTax> leerDesglose(Long documentId, Long companyId) {
        return taxJpaRepository
                .findAllByBillingDocumentIdAndCompanyIdOrderByTaxTreatmentAscTaxRateAsc(documentId,
                        companyId)
                .stream().map(taxMapper::toDomain).toList();
    }

    @Override
    public Optional<SubscriptionBillingDocument> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(entity -> mapper
                .toDomain(entity, leerDesglose(entity.getId(), entity.getCompanyId())));
    }

    @Override
    public Optional<SubscriptionBillingDocument> lockByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.lockByIdAndCompanyId(id, companyId).map(entity -> mapper
                .toDomain(entity, leerDesglose(entity.getId(), entity.getCompanyId())));
    }

    @Override
    public PageResult<SubscriptionBillingDocument> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        Sort order = Sort.by(Sort.Direction.DESC, "periodStart")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return conDesglosePaginadoDeLaEmpresa(
                jpaRepository.findAllByCompanyId(companyId, Pages.request(page, pageSize, order)),
                companyId);
    }

    @Override
    public PageResult<SubscriptionBillingDocument> findAllAwaitingExternal(int page, int pageSize) {
        // Los mas antiguos primero: la lista de trabajo se ataca por antiguedad, y el
        // desempate por id la deja estable entre paginas. Nombres de COLUMNA porque la
        // consulta es nativa: en una @Query nativa el Sort del Pageable se concatena
        // tal cual al SQL.
        Sort order = Sort.by(Sort.Direction.ASC, "created_date")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return conDesglosePaginadoCrossTenant(
                jpaRepository.findAllAwaitingExternal(Pages.request(page, pageSize, order)));
    }

    @Override
    public PageResult<SubscriptionBillingDocument> findAllOverdue(LocalDate today, int page,
            int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "due_date").and(Sort.by(Sort.Direction.ASC, "id"));
        return conDesglosePaginadoCrossTenant(
                jpaRepository.findAllOverdue(today, Pages.request(page, pageSize, order)));
    }

    @Override
    public boolean existsRecurringCycle(Long companyId, Long subscriptionId, LocalDate periodStart,
            LocalDate periodEnd) {
        return jpaRepository.countRecurringCycle(companyId, subscriptionId, periodStart,
                periodEnd) > 0;
    }

    /**
     * Convierte la página de <b>una empresa</b> añadiéndole su desglose en una sola
     * consulta más, con el {@code companyId} puesto también en esa segunda
     * consulta.
     *
     * <p>
     * Resolverlo documento a documento serían veintiuna consultas por página de
     * veinte. Es el mismo N+1 contra el que existe la regla del
     * {@code @EntityGraph}, pero aquí la relación no está mapeada como asociación
     * —el desglose se lee aparte— y por eso hay que agrupar a mano.
     *
     * <p>
     * <b>Por qué hay dos métodos donde había uno.</b> El único que existía no
     * acotaba, y servía tanto a este camino como a los dos barridos de plataforma.
     * Un solo {@code companyId} no vale para aquellos —su página mezcla clínicas a
     * propósito—, así que la salida no era añadirle el parámetro sino separar los
     * dos usos: el del tenant acota, el de plataforma declara en el nombre que no
     * acota. Con un método ambiguo, el cuarto llamador se equivoca en silencio.
     */
    private PageResult<SubscriptionBillingDocument> conDesglosePaginadoDeLaEmpresa(
            Page<SubscriptionBillingDocumentJpaEntity> page, Long companyId) {
        List<Long> ids = idsDe(page);
        Map<Long, List<BillingDocumentTax>> porDocumento = ids.isEmpty()
                ? Map.of()
                : agrupar(taxJpaRepository
                        .findAllByBillingDocumentIdInAndCompanyIdOrderByTaxTreatmentAscTaxRateAsc(
                                ids, companyId));
        return armar(page, porDocumento);
    }

    /**
     * La misma conversión para las páginas <b>cross-tenant</b> de los dos barridos
     * de plataforma, que solo sirven casos de uso cerrados a
     * {@code hasRole('SYSTEM')} a secas.
     */
    private PageResult<SubscriptionBillingDocument> conDesglosePaginadoCrossTenant(
            Page<SubscriptionBillingDocumentJpaEntity> page) {
        List<Long> ids = idsDe(page);
        Map<Long, List<BillingDocumentTax>> porDocumento = ids.isEmpty()
                ? Map.of()
                : agrupar(taxJpaRepository.findAllAcrossCompaniesByBillingDocumentIdIn(ids));
        return armar(page, porDocumento);
    }

    private static List<Long> idsDe(Page<SubscriptionBillingDocumentJpaEntity> page) {
        return page.getContent().stream().map(SubscriptionBillingDocumentJpaEntity::getId).toList();
    }

    private Map<Long, List<BillingDocumentTax>> agrupar(
            List<SubscriptionBillingDocumentTaxJpaEntity> filas) {
        return filas.stream().map(taxMapper::toDomain)
                .collect(Collectors.groupingBy(BillingDocumentTax::billingDocumentId));
    }

    private PageResult<SubscriptionBillingDocument> armar(
            Page<SubscriptionBillingDocumentJpaEntity> page,
            Map<Long, List<BillingDocumentTax>> porDocumento) {
        return Pages.result(page, entity -> mapper.toDomain(entity,
                porDocumento.getOrDefault(entity.getId(), List.of())));
    }
}
