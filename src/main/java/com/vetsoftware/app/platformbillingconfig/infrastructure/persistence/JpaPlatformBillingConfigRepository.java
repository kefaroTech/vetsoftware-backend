package com.vetsoftware.app.platformbillingconfig.infrastructure.persistence;

import com.vetsoftware.app.platformbillingconfig.application.port.out.PlatformBillingConfigRepository;
import com.vetsoftware.app.platformbillingconfig.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfig;
import com.vetsoftware.app.platformbillingconfig.domain.PriceListRef;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de la tabla singleton.
 *
 * <p>
 * Hidrata el {@link PriceListRef} con {@link PriceListQueryPort} en lugar de
 * con un {@code @EntityGraph} sobre una asociación, porque la entidad no cuelga
 * ninguna (ver {@link PlatformBillingConfigJpaEntity}). Depender de un puerto
 * de {@code application} desde {@code infrastructure} respeta la dirección
 * {@code infrastructure → application → domain}.
 *
 * <p>
 * Una tarifa apuntada que ya no se puede resolver —lista deshabilitada o
 * borrada— deja el {@code defaultPriceList} en {@code null} en vez de reventar
 * la lectura: la FK del esquema es {@code RESTRICT}, así que ese estado no
 * debería existir, y si existe conviene poder abrir el formulario para
 * arreglarlo.
 */
@Repository
public class JpaPlatformBillingConfigRepository implements PlatformBillingConfigRepository {

    /** El único valor legal de la columna discriminadora. */
    private static final byte SINGLETON = 1;

    private final PlatformBillingConfigJpaRepository jpaRepository;
    private final PlatformBillingConfigJpaMapper mapper;
    private final PriceListQueryPort priceListQueryPort;

    public JpaPlatformBillingConfigRepository(PlatformBillingConfigJpaRepository jpaRepository,
            PlatformBillingConfigJpaMapper mapper, PriceListQueryPort priceListQueryPort) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.priceListQueryPort = priceListQueryPort;
    }

    @Override
    public Optional<PlatformBillingConfig> find() {
        return jpaRepository.findBySingleton(SINGLETON).map(this::toDomain);
    }

    @Override
    public PlatformBillingConfig save(PlatformBillingConfig config) {
        return toDomain(jpaRepository.save(mapper.toJpa(config)));
    }

    private PlatformBillingConfig toDomain(PlatformBillingConfigJpaEntity entity) {
        PriceListRef ref = entity.getDefaultPriceListId() == null
                ? null
                : priceListQueryPort.findById(entity.getDefaultPriceListId()).orElse(null);
        return mapper.toDomain(entity, ref);
    }
}
