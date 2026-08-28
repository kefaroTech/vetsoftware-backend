package com.vetsoftware.app.gatewaysettlement.application.usecase;

import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementReconciliationDto;
import com.vetsoftware.app.gatewaysettlement.application.port.in.ReconcileGatewaySettlementUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.out.GatewaySettlementRepository;
import com.vetsoftware.app.gatewaysettlement.application.port.out.SettledPaymentCountPort;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlementNotFoundException;
import com.vetsoftware.app.gatewaysettlement.domain.PaymentCountReconciliation;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contrasta los cobros que el lote declara contra los que de verdad cuelgan de
 * el: <b>si dice 37 y hay 36, hay un pago perdido</b>.
 *
 * <p>
 * <strong>Las dos lecturas van en la misma transaccion de solo lectura</strong>
 * y no por ahorro: contar los pagos fuera de la transaccion en la que se leyo
 * el lote deja una ventana en la que alguien ata el cobro que falta, y el
 * informe saldria diciendo que hay 36 cuando ya hay 37. Un descuadre falso
 * manda a un operario a buscar durante una tarde un pago que no se ha perdido.
 *
 * <p>
 * <strong>No lanza cuando no cuadra.</strong> El descuadre es el resultado, no
 * un error: una excepcion dejaria precisamente las liquidaciones interesantes
 * —las unicas que hay que mirar— sin poderse consultar.
 */
@Observed(name = "gateway.settlement.reconcile")
@Service
public class ReconcileGatewaySettlementService implements ReconcileGatewaySettlementUseCase {

    private final GatewaySettlementRepository repository;
    private final SettledPaymentCountPort settledPaymentCountPort;

    public ReconcileGatewaySettlementService(GatewaySettlementRepository repository,
            SettledPaymentCountPort settledPaymentCountPort) {
        this.repository = repository;
        this.settledPaymentCountPort = settledPaymentCountPort;
    }

    @Override
    @Transactional(readOnly = true)
    public GatewaySettlementReconciliationDto reconcile(Long id) {
        GatewaySettlement settlement = repository.findById(id)
                .orElseThrow(() -> new GatewaySettlementNotFoundException(id));
        long linked = settledPaymentCountPort.countByGatewayAndSettlementReference(
                settlement.getGateway(), settlement.getSettlementReference());
        PaymentCountReconciliation reconciliation = settlement.reconcileWith(linked);
        return GatewaySettlementReconciliationDto.from(settlement, reconciliation);
    }
}
