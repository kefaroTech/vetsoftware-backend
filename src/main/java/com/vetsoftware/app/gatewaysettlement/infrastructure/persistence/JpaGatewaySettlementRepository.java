package com.vetsoftware.app.gatewaysettlement.infrastructure.persistence;

import com.vetsoftware.app.gatewaysettlement.application.port.out.GatewaySettlementRepository;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaGatewaySettlementRepository implements GatewaySettlementRepository {

    private final GatewaySettlementJpaRepository jpaRepository;
    private final GatewaySettlementJpaMapper mapper;

    public JpaGatewaySettlementRepository(GatewaySettlementJpaRepository jpaRepository,
            GatewaySettlementJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public GatewaySettlement save(GatewaySettlement settlement) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(settlement)));
    }

    @Override
    public Optional<GatewaySettlement> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByGatewayAndSettlementReference(String gateway,
            String settlementReference) {
        return jpaRepository.existsByGatewayAndSettlementReference(gateway, settlementReference);
    }

    @Override
    public PageResult<GatewaySettlement> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, masReciente())),
                mapper::toDomain);
    }

    /**
     * Lo ultimo que liquido la pasarela primero, que es como se revisa una
     * conciliacion: el lote de ayer es el que todavia no esta cuadrado.
     *
     * <p>
     * <strong>Desempate por {@code id} descendente para que el orden sea
     * total.</strong> Recorre {@code ix_gateway_settlements_settled}, y la pasarela
     * liquida varios lotes con la <em>misma</em> {@code settled_on} —uno por
     * moneda, uno por cuenta—, asi que sin desempate dos paginas consecutivas
     * repetirian u omitirian filas. Aqui una fila omitida es un lote entero fuera
     * del cuadre: sesenta cobros que nadie concilia.
     */
    private static Sort masReciente() {
        return Sort.by(Sort.Direction.DESC, "settledOn").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
