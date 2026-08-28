package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaExternalInvoicingOutageRepository implements ExternalInvoicingOutageRepository {

    private final ExternalInvoicingOutageJpaRepository jpaRepository;
    private final ExternalInvoicingOutageJpaMapper mapper;

    public JpaExternalInvoicingOutageRepository(ExternalInvoicingOutageJpaRepository jpaRepository,
            ExternalInvoicingOutageJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}</strong>, y la diferencia se
     * ve justo en el campo que este adaptador devuelve: {@code save} no incrementa
     * {@code @Version} hasta el flush, asi que la respuesta de un cierre saldria
     * con la version <em>anterior</em>. Aqui esa version no se publica —ni el DTO
     * ni el response la llevan—, pero el flush temprano tiene un segundo efecto que
     * si importa: hace que la violacion de {@code uq_eio_open} salte <b>dentro</b>
     * del caso de uso, donde se puede traducir, y no al cerrar la transaccion, ya
     * fuera de todo manejador de dominio.
     */
    @Override
    public ExternalInvoicingOutage save(ExternalInvoicingOutage outage) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(outage)));
    }

    @Override
    public Optional<ExternalInvoicingOutage> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<ExternalInvoicingOutage> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, historyOrder())),
                mapper::toDomain);
    }

    @Override
    public List<ExternalInvoicingOutage> findAllOpen() {
        return jpaRepository.findByEndedAtIsNull(openOrder()).stream().map(mapper::toDomain)
                .toList();
    }

    /**
     * El historico, de la mas reciente a la mas antigua, con el {@code id} de
     * desempate.
     *
     * <p>
     * El desempate no es adorno: dos caidas de causantes distintos pueden empezar
     * en el mismo instante —un corte de red se lleva por delante al emisor externo—
     * y sin un criterio estable dos paginas consecutivas pueden repetir u omitir
     * filas.
     */
    private static Sort historyOrder() {
        return Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id"));
    }

    /**
     * Las vivas, la mas antigua primero: la que lleva mas tiempo caida es la que
     * hay que mirar. Con {@code id} de desempate por el mismo motivo, aunque aqui
     * el resultado no se pagina.
     */
    private static Sort openOrder() {
        return Sort.by(Sort.Order.asc("startedAt"), Sort.Order.asc("id"));
    }
}
