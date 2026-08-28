package com.vetsoftware.app.gatewaysettlement.application.usecase;

import com.vetsoftware.app.gatewaysettlement.application.command.AttachProviderInvoiceCommand;
import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import com.vetsoftware.app.gatewaysettlement.application.port.in.AttachProviderInvoiceUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.out.GatewaySettlementRepository;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlementNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escribe el soporte del gasto sobre un lote ya cargado.
 *
 * <p>
 * <strong>{@code @Transactional} porque son dos operaciones de
 * repositorio</strong> —{@code findById} y {@code save}— y porque el
 * {@code @Version} de la entidad solo protege el ciclo leer-modificar-guardar
 * completo: partido en dos transacciones, dos operarios escribiendo la factura
 * del mismo lote a la vez se pisarian sin excepcion y sin log.
 *
 * <p>
 * <strong>La carga es ancha porque no existe otra.</strong> Lo que exime a este
 * servicio de {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} no es una excepcion
 * escrita a mano: es que el puerto de salida no declara ninguna variante
 * acotada que este servicio pudiera estar ignorando, y no la declara porque la
 * tabla no tiene empresa.
 */
@Observed(name = "gateway.settlement.attach.provider.invoice")
@Service
public class AttachProviderInvoiceService implements AttachProviderInvoiceUseCase {

    private final GatewaySettlementRepository repository;

    public AttachProviderInvoiceService(GatewaySettlementRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public GatewaySettlementDto execute(AttachProviderInvoiceCommand command) {
        GatewaySettlement settlement = repository.findById(command.id())
                .orElseThrow(() -> new GatewaySettlementNotFoundException(command.id()));
        settlement.attachProviderInvoice(command.providerInvoiceRef(), command.providerTaxId());
        return GatewaySettlementDto.from(repository.save(settlement));
    }
}
