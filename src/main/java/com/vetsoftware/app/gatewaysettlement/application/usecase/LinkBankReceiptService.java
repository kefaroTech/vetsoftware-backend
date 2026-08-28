package com.vetsoftware.app.gatewaysettlement.application.usecase;

import com.vetsoftware.app.gatewaysettlement.application.command.LinkBankReceiptCommand;
import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import com.vetsoftware.app.gatewaysettlement.application.port.in.LinkBankReceiptUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.out.BankReceiptValidationPort;
import com.vetsoftware.app.gatewaysettlement.application.port.out.GatewaySettlementRepository;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlementNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ata el lote a la linea del extracto por la que entro su neto.
 *
 * <p>
 * <strong>La entrada de banco se valida antes de escribirla</strong> aunque
 * {@code fk_gateway_settlements_receipt} la vigile igual: sin esta
 * comprobacion, un id inexistente sale como un fallo de integridad del driver
 * —un 500 que no dice cual de las dos partes falta— en vez del 400 que le
 * explica al operario que la linea del extracto que escribio no existe.
 *
 * <p>
 * <strong>El {@code IllegalArgumentException} lo lanza este servicio y no el
 * adaptador.</strong> El puerto de validacion devuelve un {@code boolean} a
 * proposito: el CLAUDE.md prohibe que un adaptador de persistencia lance por
 * una clave foranea que no encuentra, porque entonces la decision de si eso es
 * un 400, un 404 o un 409 quedaria tomada en la capa que menos sabe.
 */
@Observed(name = "gateway.settlement.link.bank.receipt")
@Service
public class LinkBankReceiptService implements LinkBankReceiptUseCase {

    private final GatewaySettlementRepository repository;
    private final BankReceiptValidationPort bankReceiptValidationPort;

    public LinkBankReceiptService(GatewaySettlementRepository repository,
            BankReceiptValidationPort bankReceiptValidationPort) {
        this.repository = repository;
        this.bankReceiptValidationPort = bankReceiptValidationPort;
    }

    @Override
    @Transactional
    public GatewaySettlementDto execute(LinkBankReceiptCommand command) {
        GatewaySettlement settlement = repository.findById(command.id())
                .orElseThrow(() -> new GatewaySettlementNotFoundException(command.id()));
        if (!bankReceiptValidationPort.exists(command.bankReceiptId()))
            throw new IllegalArgumentException(
                    "Bank receipt not found: " + command.bankReceiptId());
        settlement.linkBankReceipt(command.bankReceiptId());
        return GatewaySettlementDto.from(repository.save(settlement));
    }
}
