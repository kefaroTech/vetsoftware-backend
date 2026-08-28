package com.vetsoftware.app.gatewaysettlement.infrastructure.persistence;

import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.gatewaysettlement.domain.SettlementAmounts;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Un solo {@code toDomain} y sin sobrecarga de camino de
 * escritura</strong>: el dominio no guarda ningun companion VO —la entrada de
 * banco viaja como {@code Long}— asi que no hay proxy que se pueda disparar al
 * reconstruir el lote.
 *
 * <p>
 * <strong>Los cinco importes se aplanan en la ida y se vuelven a agrupar en la
 * vuelta.</strong> La identidad {@code net = gross - fee - feeTax - gmf} se
 * revalida en el constructor de {@code SettlementAmounts} cada vez que una fila
 * sube de la base: si alguien escribiera por SQL crudo un lote que no cuadra
 * —saltandose el {@code CHECK} es imposible, pero un changeset que lo relajara
 * si—, se veria al leerlo y no tres informes despues.
 *
 * <p>
 * <strong>La {@code version} viaja en los dos sentidos.</strong> Sin llevarla
 * en la ida, cada {@code save} de un lote ya persistido le pasaria a Hibernate
 * una version nula y la operacion se convertiria en un {@code INSERT}: el
 * bloqueo optimista dejaria de proteger nada justo en las dos operaciones que
 * mutan el lote.
 */
@Component
public class GatewaySettlementJpaMapper {

    public GatewaySettlementJpaEntity toJpa(GatewaySettlement settlement) {
        SettlementAmounts amounts = settlement.getAmounts();
        GatewaySettlementJpaEntity entity = new GatewaySettlementJpaEntity();
        entity.setId(settlement.getId());
        entity.setGateway(settlement.getGateway());
        entity.setSettlementReference(settlement.getSettlementReference());
        entity.setProviderInvoiceRef(settlement.getProviderInvoiceRef());
        entity.setProviderTaxId(settlement.getProviderTaxId());
        entity.setGrossAmount(amounts.gross());
        entity.setFeeAmount(amounts.fee());
        entity.setFeeTaxAmount(amounts.feeTax());
        entity.setGmfAmount(amounts.gmf());
        entity.setNetAmount(amounts.net());
        entity.setPaymentCount(settlement.getPaymentCount());
        entity.setSettledOn(settlement.getSettledOn());
        entity.setBankReceiptId(settlement.getBankReceiptId());
        entity.setCreatedDate(settlement.getCreatedDate());
        entity.setVersion(settlement.getVersion());
        return entity;
    }

    public GatewaySettlement toDomain(GatewaySettlementJpaEntity entity) {
        SettlementAmounts amounts = new SettlementAmounts(entity.getGrossAmount(),
                entity.getFeeAmount(), entity.getFeeTaxAmount(), entity.getGmfAmount(),
                entity.getNetAmount());
        return new GatewaySettlement(entity.getId(), entity.getGateway(),
                entity.getSettlementReference(), entity.getProviderInvoiceRef(),
                entity.getProviderTaxId(), amounts, entity.getPaymentCount(), entity.getSettledOn(),
                entity.getBankReceiptId(), entity.getCreatedDate(), entity.getVersion());
    }
}
