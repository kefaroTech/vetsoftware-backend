package com.vetsoftware.app.customercredit.infrastructure.persistence;

import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.customercredit.domain.CreditEntryKind;
import com.vetsoftware.app.customercredit.domain.CreditLot;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCustomerCreditEntryRepository implements CustomerCreditEntryRepository {

    /** Los asientos que restan de un lote, y por tanto los que lo netean. */
    private static final List<CreditEntryKind> LOT_CONSUMERS = List.of(CreditEntryKind.CONSUMPTION,
            CreditEntryKind.EXPIRATION);

    /**
     * Orden del reparto: <strong>primero el que antes caduca</strong>, los sin
     * fecha al final, desempate por id. No es cosmetico — es D-71, y consumir en
     * otro orden hace caducar saldo que se podia haber gastado.
     */
    private static final Comparator<CreditLot> BY_NEAREST_EXPIRY = Comparator
            .comparing(CreditLot::expiresOn, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(CreditLot::entryId);

    private final CustomerCreditEntryJpaRepository jpaRepository;
    private final CustomerCreditEntryJpaMapper mapper;

    public JpaCustomerCreditEntryRepository(CustomerCreditEntryJpaRepository jpaRepository,
            CustomerCreditEntryJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CustomerCreditEntry save(CustomerCreditEntry entry) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(entry)));
    }

    @Override
    public Optional<CustomerCreditEntry> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<CustomerCreditEntry> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId) {
        return jpaRepository.findByCompanyIdAndClientRequestId(companyId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public List<CustomerCreditEntry> findOperation(Long companyId, String clientRequestId) {
        return jpaRepository
                .findByCompanyIdAndClientRequestIdStartingWithOrderByIdAsc(companyId,
                        clientRequestId + OPERATION_SEPARATOR)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CreditLot> findOpenLotsByCompanyId(Long companyId) {
        return netLots(companyId, lot -> true);
    }

    /**
     * Vencido es <strong>estrictamente anterior</strong> a la fecha de corte: un
     * lote que caduca hoy todavia se puede gastar hoy. La alternativa —caducarlo el
     * mismo dia que vence— le quita al cliente un dia que la fecha le prometia.
     */
    @Override
    public List<CreditLot> findExpiredLotsByCompanyId(Long companyId, LocalDate asOf) {
        return netLots(companyId, lot -> lot.expiresOn() != null && lot.expiresOn().isBefore(asOf));
    }

    /**
     * Netea el libro por lote: cada alta menos lo que ya se le consumio y lo que ya
     * se le caduco.
     *
     * <p>
     * <strong>Se netea en memoria y no en SQL, a sabiendas.</strong> El saldo a
     * favor de una clinica son decenas de filas, no millones, y una consulta con
     * {@code GROUP BY} y {@code HAVING} sobre la misma tabla auto-referenciada es
     * mucho mas facil de escribir mal —y de que envejezca mal— que dos lecturas
     * acotadas por empresa y una suma. Las dos lecturas van acotadas, que es lo que
     * importa para el aislamiento. Si algun dia una empresa acumula un historico
     * que lo justifique, el reemplazo es una consulta agregada respaldada por
     * {@code ix_cce_lot}, y este metodo es el unico sitio que habria que tocar.
     *
     * <p>
     * <strong>Los asientos {@code VOID} y {@code CORRECTION} no netean ningun
     * lote</strong> porque no pertenecen a ninguno ({@code chk_cce_lot} los deja
     * fuera): mueven el saldo global sin tocar el reparto. Una correccion que baje
     * el saldo por debajo de la suma de los lotes vivos aparece como la divergencia
     * que detecta el consumo, y eso es lo correcto — manda el libro y la proyeccion
     * se rehace.
     */
    private List<CreditLot> netLots(Long companyId, Predicate<CreditLot> filter) {
        Map<Long, BigDecimal> consumedByLot = new HashMap<>();
        for (CustomerCreditEntryJpaEntity consumer : jpaRepository
                .findByCompanyIdAndEntryKindIn(companyId, LOT_CONSUMERS)) {
            if (consumer.getLotEntryId() == null)
                continue;
            consumedByLot.merge(consumer.getLotEntryId(), consumer.getAmount(), BigDecimal::add);
        }

        List<CreditLot> lots = new ArrayList<>();
        for (CustomerCreditEntryJpaEntity grant : jpaRepository
                .findByCompanyIdAndEntryKind(companyId, CreditEntryKind.GRANT)) {
            // Los consumos son negativos, asi que el remanente es una suma.
            BigDecimal remaining = grant.getAmount()
                    .add(consumedByLot.getOrDefault(grant.getId(), BigDecimal.ZERO));
            if (remaining.signum() <= 0)
                continue;
            CreditLot lot = new CreditLot(grant.getId(), remaining, grant.getExpiresOn());
            if (filter.test(lot))
                lots.add(lot);
        }
        lots.sort(BY_NEAREST_EXPIRY);
        return lots;
    }

    @Override
    public PageResult<CustomerCreditEntry> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(
                jpaRepository.findAllByCompanyId(companyId, Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    @Override
    public PageResult<CustomerCreditEntry> findAllExpiringBefore(LocalDate before, int page,
            int pageSize) {
        Sort byExpiry = Sort.by(Sort.Direction.ASC, "expiresOn")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(jpaRepository.findAllExpiringBefore(CreditEntryKind.GRANT, before,
                Pages.request(page, pageSize, byExpiry)), mapper::toDomain);
    }

    @Override
    public PageResult<CustomerCreditEntry> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    /**
     * Lo mas reciente primero, con el {@code id} de desempate. Sin desempate, dos
     * asientos del mismo microsegundo pueden salir en dos paginas o en ninguna.
     */
    private static Sort order() {
        return Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
