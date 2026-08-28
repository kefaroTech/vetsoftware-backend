package com.vetsoftware.app.gatewaysettlement.infrastructure.persistence;

import com.vetsoftware.app.gatewaysettlement.application.port.out.SettledPaymentCountPort;
import org.springframework.stereotype.Component;

/**
 * Cuenta los cobros atados al lote. Es el unico punto de la rodaja que mira la
 * tabla de otra feature, y lo hace a traves del contador nativo declarado en
 * {@link GatewaySettlementJpaRepository#countSettledPayments(String, String)}.
 *
 * <p>
 * <strong>Por que no inyecta el repositorio de la otra rodaja.</strong> El
 * cruce estaria permitido —{@code infrastructure/persistence} puede importar el
 * {@code XxxJpaRepository} de otra feature—, pero
 * {@code SubscriptionPaymentJpaEntity} <b>no mapea
 * {@code settlement_reference}</b>: la columna existe en el esquema desde el
 * changeset 252 y la entidad solo declara {@code gateway} y
 * {@code gateway_reference}. No hay consulta derivada posible sobre un campo
 * que el modelo no tiene, y anadirle una {@code @Query} al repositorio de
 * aquella rodaja pondria una consulta de esta feature en un archivo que es de
 * otra.
 */
@Component
public class JpaSettledPaymentCountPort implements SettledPaymentCountPort {

    private final GatewaySettlementJpaRepository jpaRepository;

    public JpaSettledPaymentCountPort(GatewaySettlementJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public long countByGatewayAndSettlementReference(String gateway, String settlementReference) {
        return jpaRepository.countSettledPayments(gateway, settlementReference);
    }
}
