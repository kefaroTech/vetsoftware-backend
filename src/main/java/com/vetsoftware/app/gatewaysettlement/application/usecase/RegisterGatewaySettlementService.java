package com.vetsoftware.app.gatewaysettlement.application.usecase;

import com.vetsoftware.app.gatewaysettlement.application.command.RegisterGatewaySettlementCommand;
import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import com.vetsoftware.app.gatewaysettlement.application.port.in.RegisterGatewaySettlementUseCase;
import com.vetsoftware.app.gatewaysettlement.application.port.out.GatewaySettlementRepository;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlementAlreadyRegisteredException;
import com.vetsoftware.app.gatewaysettlement.domain.SettlementAmounts;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga una liquidacion de la pasarela.
 *
 * <p>
 * <strong>La comprobacion previa de duplicado no sustituye a la unicidad de la
 * base: la traduce.</strong> {@code uq_gateway_settlements_reference} sigue
 * siendo lo unico que garantiza que no entren dos, porque entre el
 * {@code exists} y el {@code insert} cabe otra transaccion. Lo que aporta es
 * que el caso comun —el operario que vuelve a cargar el informe del mes que ya
 * cargo— conteste un 409 con la pasarela y la referencia en el mensaje en vez
 * de un 500 con un {@code Duplicate entry} del driver.
 *
 * <p>
 * <strong>El {@code SettlementAmounts} se arma aqui y no en el
 * controller.</strong> Es donde vive la identidad
 * {@code net = gross - fee - feeTax - gmf} y donde tiene que fallar si no
 * cuadra: construirlo en la capa web haria que una invariante de dominio se
 * evaluara en el binder, y el mensaje que veria el operario dejaria de nombrar
 * los dos numeros que no cuadran.
 *
 * <p>
 * <strong>La fecha de creacion sale del {@code Clock} inyectado</strong> y no
 * de un {@code LocalDateTime.now()} pelado: con el contenedor en horario
 * universal, entre las siete de la tarde y la medianoche «hoy» ya es manana en
 * Bogota, y un lote cargado el 31 quedaria sellado el 1 del mes siguiente — en
 * el mes contable equivocado. {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el
 * build por ello.
 */
@Observed(name = "gateway.settlement.register")
@Service
public class RegisterGatewaySettlementService implements RegisterGatewaySettlementUseCase {

    private final GatewaySettlementRepository repository;
    private final Clock clock;

    public RegisterGatewaySettlementService(GatewaySettlementRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public GatewaySettlementDto execute(RegisterGatewaySettlementCommand command) {
        if (repository.existsByGatewayAndSettlementReference(command.gateway(),
                command.settlementReference()))
            throw new GatewaySettlementAlreadyRegisteredException(command.gateway(),
                    command.settlementReference());

        SettlementAmounts amounts = new SettlementAmounts(command.grossAmount(),
                command.feeAmount(), command.feeTaxAmount(), command.gmfAmount(),
                command.netAmount());
        GatewaySettlement settlement = GatewaySettlement.register(command.gateway(),
                command.settlementReference(), amounts, command.paymentCount(), command.settledOn(),
                LocalDateTime.now(clock));
        return GatewaySettlementDto.from(repository.save(settlement));
    }
}
